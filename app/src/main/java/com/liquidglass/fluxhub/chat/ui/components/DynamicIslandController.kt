package com.liquidglass.fluxhub.chat.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 全局灵动岛控制器
 * 单例对象，可从应用任何地方触发灵动岛通知
 */
object DynamicIslandController {
    
    // ========== 状态 ==========
    var state by mutableStateOf(DynamicIslandState.Hidden)
        private set
    
    var title by mutableStateOf("正在思考...")
        private set
    
    var modelName by mutableStateOf<String?>(null)
        private set
    
    var assistantAvatar by mutableStateOf<String?>(null)
        private set
    
    var tokenCount by mutableIntStateOf(0)
        private set
    
    var elapsedSeconds by mutableIntStateOf(0)
        private set
    
    var isCompleted by mutableStateOf(false)
        private set
    
    var isFailed by mutableStateOf(false)
        private set
    
    var successMessage by mutableStateOf("完成")
        private set

    var triggerId by mutableStateOf(0L)
        private set
    
    // ========== 设置（从 ViewModel 同步）==========
    var isEnabled by mutableStateOf(true)
    var showTokenCountEnabled by mutableStateOf(true)
    var showElapsedTimeEnabled by mutableStateOf(true)
    var loginNotificationMode by mutableStateOf("first")
    
    // ========== 内部状态 ==========
    private var timerJob: Job? = null
    private var autoHideJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    
    // ========== 公共 API ==========
    
    /**
     * 显示加载状态（用于 AI 生成等）
     */
    fun showLoading(
        title: String = "正在思考...",
        modelName: String? = null,
        avatar: String? = null
    ) {
        if (!isEnabled) return
        
        triggerId++
        this.title = title
        this.modelName = modelName
        this.assistantAvatar = avatar
        this.isCompleted = false
        this.isFailed = false
        this.tokenCount = 0
        this.elapsedSeconds = 0
        this.state = DynamicIslandState.Collapsed
        
        // 启动计时器
        startTimer()
    }
    
    /**
     * 更新 Token 计数
     */
    fun updateTokenCount(count: Int) {
        this.tokenCount = count
    }
    
    /**
     * 增加 Token 计数
     */
    fun incrementTokenCount(delta: Int = 1) {
        this.tokenCount += delta
    }
    
    /**
     * 显示成功状态
     */
    fun showSuccess(
        message: String = "完成",
        autoHideDelayMs: Long = 2500,
        avatar: String? = null,
        customTitle: String? = null
    ) {
        if (!isEnabled) return
        
        triggerId++
        stopTimer()
        this.successMessage = message
        this.isCompleted = true
        this.isFailed = false
        // 设置自定义头像和标题（用于登录通知等）
        if (avatar != null) this.assistantAvatar = avatar
        if (customTitle != null) this.title = customTitle
        this.modelName = null // 清空模型名称
        this.state = DynamicIslandState.Collapsed
        
        // 自动隐藏
        scheduleAutoHide(autoHideDelayMs)
    }
    
    /**
     * 显示错误状态
     */
    fun showError(message: String = "失败", autoHideDelayMs: Long = 2500) {
        if (!isEnabled) return
        
        triggerId++
        stopTimer()
        this.successMessage = message
        this.isCompleted = false
        this.isFailed = true
        this.state = DynamicIslandState.Collapsed
        
        // 自动隐藏
        scheduleAutoHide(autoHideDelayMs)
    }
    
    /**
     * 隐藏灵动岛
     */
    fun hide() {
        stopTimer()
        autoHideJob?.cancel()
        this.state = DynamicIslandState.Hidden
        this.isCompleted = false
        this.isFailed = false
    }
    
    /**
     * 展开灵动岛（显示详情）
     */
    fun expand() {
        if (state != DynamicIslandState.Hidden) {
            state = DynamicIslandState.Expanded
        }
    }
    
    /**
     * 收起灵动岛
     */
    fun collapse() {
        if (state != DynamicIslandState.Hidden) {
            state = DynamicIslandState.Collapsed
        }
    }
    
    /**
     * 显示长按菜单
     */
    fun showLongPressMenu() {
        if (state != DynamicIslandState.Hidden) {
            state = DynamicIslandState.LongPressMenu
        }
    }
    
    // ========== 内部方法 ==========
    
    private fun startTimer() {
        timerJob?.cancel()
        elapsedSeconds = 0
        timerJob = scope.launch {
            while (true) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }
    
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
    
    private fun scheduleAutoHide(delayMs: Long) {
        autoHideJob?.cancel()
        autoHideJob = scope.launch {
            delay(delayMs)
            hide()
        }
    }
    
    /**
     * 生成当前状态的 DynamicIslandData
     */
    fun toData(): DynamicIslandData {
        return DynamicIslandData(
            title = title,
            modelName = modelName,
            assistantAvatar = assistantAvatar,
            state = state,
            tokenCount = tokenCount,
            elapsedSeconds = elapsedSeconds,
            isCompleted = isCompleted,
            isFailed = isFailed,
            successMessage = successMessage,
            showTokenCount = showTokenCountEnabled,
            showElapsedTime = showElapsedTimeEnabled,
            triggerId = triggerId
        )
    }
}
