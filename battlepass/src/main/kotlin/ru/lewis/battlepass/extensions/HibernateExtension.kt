package ru.lewis.battlepass.extensions

import org.hibernate.Session
import org.hibernate.SessionFactory

inline fun <T> SessionFactory.fromTransaction(crossinline block: (Session) -> T): T {
    return openSession().use { session ->
        val transaction = session.beginTransaction()
        try {
            val result = block(session)
            transaction.commit()
            result
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        }
    }
}

inline fun SessionFactory.inTransaction(crossinline block: (Session) -> Unit) {
    openSession().use { session ->
        val transaction = session.beginTransaction()
        try {
            block(session)
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        }
    }
}
