package com.liquidglass.fluxhub.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.liquidglass.fluxhub.data.model.Conversation
import com.liquidglass.fluxhub.data.model.Message
import com.liquidglass.fluxhub.data.model.Provider

@Database(
    entities = [Message::class, Conversation::class, Provider::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun providerDao(): ProviderDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fluxhub_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
