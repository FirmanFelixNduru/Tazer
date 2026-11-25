package com.tazer.taskorganizer.timer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.tazer.taskorganizer.MainActivity
import com.tazer.taskorganizer.data.WorkSession

enum class FocusLinkKind { REMINDER, ACTIVE, EXTEND }

data class FocusLink(val uid: String, val id: String, val revision: Long, val kind: FocusLinkKind)

internal enum class FocusEventKind { REMINDER, ACTIVE, EXTEND, START_ALARM, CUTOFF, DONE }

internal data class FocusEvent(
    val uid: String,
    val id: String,
    val revision: Long,
    val kind: FocusEventKind,
) {
    constructor(session: WorkSession, kind: FocusEventKind) :
        this(session.ownerUid, session.id, session.revision, kind)

    fun uri(authority: String): Uri = Uri.Builder()
        .scheme("tazer-focus")
        .authority(authority)
        .appendPath(kind.name)
        .appendPath(uid)
        .appendPath(id)
        .appendPath(revision.toString())
        .build()

    fun receiverIntent(context: Context): Intent =
        Intent(context, FocusReceiver::class.java).setAction(ACTION_EVENT).setData(uri("event"))

    fun activityIntent(context: Context, linkToken: String): Intent =
        Intent(context, MainActivity::class.java).setAction(ACTION_LINK).setData(uri("link"))
            .putExtra(EXTRA_LINK_TOKEN, linkToken)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    companion object {
        const val ACTION_EVENT = "com.tazer.taskorganizer.timer.EVENT"
        private const val ACTION_LINK = "com.tazer.taskorganizer.timer.LINK"
        private const val EXTRA_LINK_TOKEN = "com.tazer.taskorganizer.timer.LINK_TOKEN"

        fun fromReceiver(intent: Intent): FocusEvent? {
            if (intent.action != ACTION_EVENT) return null
            return fromUri(intent.data, "event")?.takeIf {
                it.kind == FocusEventKind.START_ALARM || it.kind == FocusEventKind.CUTOFF ||
                    it.kind == FocusEventKind.DONE
            }
        }

        fun readLink(context: Context, intent: Intent, linkToken: String): FocusLink? {
            if (intent.action != ACTION_LINK || intent.component != ComponentName(context, MainActivity::class.java)) {
                return null
            }
            if (intent.getStringExtra(EXTRA_LINK_TOKEN) != linkToken) return null
            val event = fromUri(intent.data, "link") ?: return null
            val kind = when (event.kind) {
                FocusEventKind.REMINDER -> FocusLinkKind.REMINDER
                FocusEventKind.ACTIVE -> FocusLinkKind.ACTIVE
                FocusEventKind.EXTEND -> FocusLinkKind.EXTEND
                else -> return null
            }
            intent.data = null
            intent.removeExtra(EXTRA_LINK_TOKEN)
            return FocusLink(event.uid, event.id, event.revision, kind)
        }

        fun fromUri(uri: Uri?, authority: String): FocusEvent? {
            if (uri?.scheme != "tazer-focus" || uri.authority != authority) return null
            val parts = uri.pathSegments
            if (parts.size != 4 || parts[1].isBlank() || parts[2].isBlank()) return null
            val kind = FocusEventKind.entries.firstOrNull { it.name == parts[0] } ?: return null
            val revision = parts[3].toLongOrNull()?.takeIf { it >= 0 } ?: return null
            return FocusEvent(parts[1], parts[2], revision, kind)
        }
    }
}
