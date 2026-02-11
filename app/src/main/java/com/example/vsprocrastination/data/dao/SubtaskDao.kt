package com.example.vsprocrastination.data.dao

import androidx.room.*
import com.example.vsprocrastination.data.model.Subtask
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {
    
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder ASC")
    fun getSubtasksForTask(taskId: Long): Flow<List<Subtask>>
    
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder ASC")
    suspend fun getSubtasksForTaskSync(taskId: Long): List<Subtask>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: Subtask): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtasks(subtasks: List<Subtask>)
    
    @Update
    suspend fun updateSubtask(subtask: Subtask)
    
    @Delete
    suspend fun deleteSubtask(subtask: Subtask)
    
    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteAllForTask(taskId: Long)
    
    @Query("UPDATE subtasks SET isCompleted = :completed WHERE id = :subtaskId")
    suspend fun setCompleted(subtaskId: Long, completed: Boolean)
    
    @Query("SELECT COUNT(*) FROM subtasks WHERE taskId = :taskId")
    suspend fun getSubtaskCount(taskId: Long): Int
    
    @Query("SELECT COUNT(*) FROM subtasks WHERE taskId = :taskId AND isCompleted = 1")
    suspend fun getCompletedSubtaskCount(taskId: Long): Int
}
