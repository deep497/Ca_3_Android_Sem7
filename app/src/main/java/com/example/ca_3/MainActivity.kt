package com.example.ca_3

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String
)

@Dao
interface DiaryDao {
    @Insert
    suspend fun insertEntry(entry: DiaryEntry)

    @Query("SELECT * FROM diary_entries ORDER BY id DESC")
    fun getAllEntries(): Flow<List<DiaryEntry>>

    @Query("SELECT COUNT(*) FROM diary_entries")
    suspend fun getEntryCount(): Int
}

@Database(entities = [DiaryEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diary_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AppDatabase.getDatabase(this).diaryDao()
        setContent {
            MaterialTheme {
                DiaryScreen(dao)
            }
        }
    }
}

@Composable
fun DiaryScreen(dao: DiaryDao) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val entries by dao.getAllEntries().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Personal Diary", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Content") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (title.isNotBlank() && content.isNotBlank()) {
                    scope.launch {
                        if (dao.getEntryCount() >= 3) {
                            message = "Limit reached! Only 3 entries allowed."
                        } else {
                            dao.insertEntry(DiaryEntry(title = title, content = content))
                            title = ""
                            content = ""
                            message = "Entry Saved"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Entry")
        }

        if (message.isNotEmpty()) {
            Text(message)
        }

        LazyColumn {
            items(entries) { entry ->
                Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(entry.title)
                        Text(entry.content)
                    }
                }
            }
        }
    }
}