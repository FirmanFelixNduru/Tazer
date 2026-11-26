package com.tazer.taskorganizer.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.withTransaction
import java.util.UUID
import kotlinx.coroutines.flow.Flow

data class SubtaskDraft(val id: String?, val title: String, val completed: Boolean)

@Dao
internal interface SubtaskDao {
    @Query("SELECT * FROM Subtask WHERE ownerUid = :uid ORDER BY taskId, position, id")
    fun observe(uid: String): Flow<List<Subtask>>

    @Query("SELECT * FROM Subtask WHERE ownerUid = :uid AND taskId = :taskId ORDER BY position, id")
    suspend fun forTask(uid: String, taskId: String): List<Subtask>

    @Upsert
    suspend fun put(item: Subtask)

    @Query("UPDATE Subtask SET completed = :completed WHERE ownerUid = :uid AND taskId = :taskId AND id = :id")
    suspend fun setCompleted(uid: String, taskId: String, id: String, completed: Boolean): Int

    @Query("DELETE FROM Subtask WHERE ownerUid = :uid AND taskId = :taskId AND id = :id")
    suspend fun delete(uid: String, taskId: String, id: String): Int
}

class SubtaskRepository(private val database: TazerDatabase) {
    private val dao = database.subtaskDao()

    fun observe(uid: String) = dao.observe(uid)

    suspend fun <T> withTransaction(block: suspend () -> T): T = database.withTransaction { block() }

    suspend fun replace(uid: String, taskId: String, items: List<SubtaskDraft>) = withTransaction {
        require(items.size <= 40)
        checkNotNull(database.tazerDao().findTask(uid, taskId))
        val existing = dao.forTask(uid, taskId).associateBy(Subtask::id)
        val retained = items.mapNotNull(SubtaskDraft::id)
        require(retained.size == retained.toSet().size && retained.all(existing::containsKey))
        existing.keys.filterNot(retained::contains).forEach { dao.delete(uid, taskId, it) }
        items.forEachIndexed { position, item ->
            val row = Subtask(item.id ?: UUID.randomUUID().toString(), uid, taskId, checkedTitle(item.title),
                item.completed, position)
            if (row != existing[row.id]) dao.put(row)
        }
    }

    suspend fun save(uid: String, taskId: String, id: String?, title: String): Boolean = withTransaction {
        if (database.tazerDao().findTask(uid, taskId) == null) return@withTransaction false
        val rows = dao.forTask(uid, taskId)
        val text = checkedTitle(title)
        if (id == null) {
            if (rows.size >= 40) return@withTransaction false
            dao.put(Subtask(UUID.randomUUID().toString(), uid, taskId, text, false,
                (rows.maxOfOrNull(Subtask::position) ?: -1) + 1))
        } else {
            val existing = rows.find { it.id == id } ?: return@withTransaction false
            if (text != existing.title) dao.put(existing.copy(title = text))
        }
        true
    }

    suspend fun setCompleted(uid: String, taskId: String, id: String, completed: Boolean) =
        dao.setCompleted(uid, taskId, id, completed) == 1

    suspend fun delete(uid: String, taskId: String, id: String) = dao.delete(uid, taskId, id) == 1

    private fun checkedTitle(title: String): String = title.trim().also { require(it.length in 1..120) }
}
