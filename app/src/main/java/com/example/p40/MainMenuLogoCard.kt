package com.example.p40

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView

import androidx.cardview.widget.CardView
import kotlin.math.abs

/**
 * 메인 메뉴의 로고 카드 관리 클래스
 * 카드 문양 변경 및 스와이프 처리를 담당
 */
class MainMenuLogoCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val TAG = "MainMenuLogoCard"

    // 뷰 요소들
    private lateinit var cardView: CardView
    private lateinit var cardSymbolCenter: TextView
    private lateinit var cardSymbolTopLeft: TextView
    private lateinit var cardRankTopLeft: TextView
    private lateinit var cardSymbolBottomRight: TextView
    private lateinit var cardRankBottomRight: TextView
    
    // 카드 문양 상태
    private var currentSymbol: CardSymbolType = CardSymbolType.SPADE
    
    // 제스처 감지
    private lateinit var gestureDetector: GestureDetector
    
    // 애니메이션 참조
    private var currentAnimation: Animation? = null
    private var isAnimationPlaying = false
    
    // ObjectAnimator 참조 - 회전 애니메이션을 위해 추가
    private var rotationAnimator: ObjectAnimator? = null
    
    // 드래그 관련 변수
    private var isDragging = false
    private var initialX = 0f
    private var initialY = 0f
    private var dX = 0f
    private var dY = 0f
    
    // 카드 플링 상태
    private var isCardFlinging = false
    
    // 초기화
    init {
        // 로고 카드 뷰 초기화
        initView()
        
        // 제스처 감지 설정
        setupGestureDetector()
        
        // 초기 카드 문양 설정
        updateCardSymbol()
        
        // 클릭 및 터치 이벤트 활성화
        isClickable = true
        isFocusable = true
        
        // 클릭 리스너 직접 설정 - 강화된 처리
        setOnClickListener {
            Log.d(TAG, "Direct onClick called on MainMenuLogoCard")
            // 명시적 클릭 처리 - 모든 애니메이션 상태를 확인하고 강제 토글
            if (!isCardFlinging && !isDragging) {
                Log.d(TAG, "Processing click - toggling animation with force")
                // 현재 애니메이션 상태와 관계없이 강제로 토글
                forceToggleAnimation()
            } else {
                Log.d(TAG, "Ignoring click - card is flinging or dragging")
            }
        }
        
        Log.d(TAG, "MainMenuLogoCard initialized with enhanced click listener")
    }
    
    /**
     * 로고 카드 뷰 초기화
     */
    private fun initView() {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.logo_card_content, this, true)
        
        try {
            // 클리핑 비활성화 - 회전 시 카드가 잘리지 않도록 함
            clipChildren = false
            clipToPadding = false
            
            // 부모 뷰에도 클리핑 비활성화 설정 요청 - 모든 상위 뷰에 적용
            var parent = parent
            while (parent != null) {
                if (parent is ViewGroup) {
                    parent.clipChildren = false
                    parent.clipToPadding = false
                }
                parent = parent.parent
            }
            
            // 가시성 확인
            visibility = View.VISIBLE
            
            // 뷰 요소 참조 가져오기
            cardView = findViewById(R.id.cardView)
            cardSymbolCenter = findViewById(R.id.tvCardSymbolCenter)
            cardSymbolTopLeft = findViewById(R.id.tvCardSymbolTopLeft)
            cardRankTopLeft = findViewById(R.id.tvCardRankTopLeft)
            cardSymbolBottomRight = findViewById(R.id.tvCardSymbolBottomRight)
            cardRankBottomRight = findViewById(R.id.tvCardRankBottomRight)
            
            // 배경 및 가시성 설정 - CardView 자체도 클리핑 비활성화
            cardView.clipChildren = false
            cardView.clipToPadding = false
            cardView.visibility = View.VISIBLE
            
            Log.d(TAG, "View elements initialized successfully with clipping disabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing view elements: ${e.message}")
        }
    }
    
    /**
     * 제스처 감지 설정
     */
    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            // 플링(스와이프) 감지
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                
                // 카드를 드래그 중인 경우는 flingCardAway에서 처리하므로 여기서는 무시
                if (isDragging) {
                    return false
                }
                
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                
                // 수직 스와이프가 수평 스와이프보다 더 뚜렷할 때만 처리
                if (abs(diffY) > abs(diffX)) {
                    if (diffY < 0) { // 위로 스와이프
                        Log.d(TAG, "Up swipe detected, changing to next symbol")
                        changeToNextSymbol()
                        return true
                    } else if (diffY > 0) { // 아래로 스와이프
                        Log.d(TAG, "Down swipe detected, changing to previous symbol")
                        changeToPreviousSymbol()
                        return true
                    }
                }
                return false
            }
            
            // 일반 탭 감지 - 이것도 forceToggleAnimation으로 처리
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (!isDragging && !isCardFlinging) {
                    Log.d(TAG, "Single tap up detected via GestureDetector")
                    // 명시적으로 forceToggleAnimation 호출
                    forceToggleAnimation()
                    return true
                }
                return false
            }
            
            // 드래그 시작 감지
            override fun onDown(e: MotionEvent): Boolean {
                if (!isCardFlinging) {
                    // 드래그 시작 위치 저장
                    initialX = cardView.x
                    initialY = cardView.y
                    dX = 0f
                    dY = 0f
                }
                return true
            }
        })
        
        Log.d(TAG, "Gesture detector set up with enhanced tap handling")
    }
    
    private fun showToast(message: String) {
        // Toast 제거
    }
    
    /**
     * 상위 뷰의 터치 이벤트 가로채기 방지
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return true // 터치 이벤트를 가로채서 자식 뷰가 처리하도록 함
    }
    
    /**
     * 터치 이벤트 처리
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 카드가 날아가는 중이면 이벤트 무시
        if (isCardFlinging) {
            return true
        }
        
        // 제스처 감지기에 이벤트 전달
        val gestureResult = gestureDetector.onTouchEvent(event)
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 드래그 시작
                isDragging = true
                if (isAnimationPlaying) {
                    forceStopAllAnimations() // 드래그 시 애니메이션 중지 - 개선된 메서드 사용
                }
                
                // 드래그 시작 위치 기록
                initialX = cardView.x
                initialY = cardView.y
                dX = event.rawX - cardView.x
                dY = event.rawY - cardView.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    // 이동 거리 계산
                    val newX = event.rawX - dX
                    val newY = event.rawY - dY
                    val distanceX = newX - initialX
                    val distanceY = newY - initialY
                    val distance = Math.sqrt((distanceX * distanceX + distanceY * distanceY).toDouble()).toFloat()
                    
                    // 매우 작은 움직임은 무시하고 드래그로 처리하지 않음
                    if (distance < 5) {
                        return true
                    }
                    
                    // 카드 위치 업데이트
                    // 이동 제한 (너무 멀리 가지 않도록)
                    val maxDistance = width / 1.5f
                    
                    if (distance <= maxDistance) {
                        cardView.x = newX
                        cardView.y = newY
                    } else {
                        // 최대 거리로 제한
                        val ratio = maxDistance / distance
                        cardView.x = initialX + distanceX * ratio
                        cardView.y = initialY + distanceY * ratio
                    }
                    
                    // 드래그에 따라 회전 효과 추가 (기울기)
                    val rotationFactor = 15f // 최대 회전 각도
                    cardView.rotation = (cardView.x - initialX) / width * rotationFactor
                    
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasDragging = isDragging
                isDragging = false
                
                // 카드를 놓았을 때의 위치
                val distanceX = cardView.x - initialX
                val distanceY = cardView.y - initialY
                val distance = Math.sqrt((distanceX * distanceX + distanceY * distanceY).toDouble()).toFloat()
                
                // 작은 움직임은 클릭으로 처리 - 향상된 역치값 사용
                if (distance < 10) {
                    Log.d(TAG, "Small movement detected as click in ACTION_UP, distance: $distance")
                    // 명시적으로 강화된 애니메이션 토글 호출
                    forceToggleAnimation()
                    return true
                }
                
                if (wasDragging) {
                    // 일정 거리 이상 드래그하고 수직 방향 드래그가 충분한 경우만 카드 날리기
                    if (distance > width / 6) { // 화면 너비의 1/6 이상 드래그
                        // 작은 움직임이었다면 제스처 감지기에게 이벤트 전달
                        if (distance < 20 && abs(distanceX) > abs(distanceY)) {
                            // 짧은 수평 드래그는 원래 위치로 복귀
                            returnCardToOriginalPosition()
                        } else {
                            flingCardAway(distanceX, distanceY)
                        }
                    } else {
                        // 충분히 드래그하지 않은 경우 원래 위치로 복귀
                        returnCardToOriginalPosition()
                    }
                    return true
                }
            }
        }
        
        // 제스처나 특별한 처리가 없을 경우 기본 이벤트 처리
        return gestureResult || super.onTouchEvent(event)
    }
    
    /**
     * 카드를 날려보내는 애니메이션
     */
    private fun flingCardAway(distanceX: Float, distanceY: Float) {
        isCardFlinging = true
        
        // 실행 중인 모든 애니메이션 중지
        forceStopAllAnimations()
        
        // 이동 방향 결정 (현재 방향의 연장선)
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        
        // 어느 방향으로든 카드가 날아가면 다음 문양으로 변경
        val nextSymbol = currentSymbol.next()
        
        // 화면 밖으로 충분히 나가도록 목표 위치 계산
        val ratio = if (abs(distanceX) > abs(distanceY)) {
            // 수평 방향 이동이 더 큰 경우
            (if (distanceX > 0) screenWidth * 2 else -screenWidth) / distanceX
        } else {
            // 수직 방향 이동이 더 큰 경우
            (if (distanceY > 0) screenHeight * 2 else -screenHeight) / distanceY
        }
        
        val targetX = initialX + distanceX * ratio
        val targetY = initialY + distanceY * ratio
        
        // 날아가는 애니메이션
        val animDuration = 300L // 빠르게 날아가도록 300ms로 설정
        
        // 크기, 회전, 위치 변화를 동시에 적용
        val scaleDown = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.5f)
        val scaleDown2 = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.5f)
        val moveX = PropertyValuesHolder.ofFloat(View.X, cardView.x, targetX)
        val moveY = PropertyValuesHolder.ofFloat(View.Y, cardView.y, targetY)
        
        // 회전 방향도 드래그 방향에 맞게 조정
        val rotateAngle = if (abs(distanceX) > abs(distanceY)) {
            if (distanceX > 0) 180f else -180f // 수평 드래그
        } else {
            if (distanceY > 0) 180f else -180f // 수직 드래그
        }
        
        val rotate = PropertyValuesHolder.ofFloat(View.ROTATION, cardView.rotation, cardView.rotation + rotateAngle)
        
        val animator = ObjectAnimator.ofPropertyValuesHolder(
            cardView, scaleDown, scaleDown2, moveX, moveY, rotate)
        
        animator.duration = animDuration
        animator.interpolator = AccelerateInterpolator()
        
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // 카드가 날아간 후 문양 직접 설정
                currentSymbol = nextSymbol
                
                // 카드를 원래 위치로, 새 문양으로 되돌림
                cardView.x = initialX
                cardView.y = initialY
                cardView.rotation = 0f
                cardView.scaleX = 0.1f
                cardView.scaleY = 0.1f
                
                // 문양 업데이트
                updateCardSymbol()
                
                // 로그 출력으로 디버깅
                Log.d(TAG, "Card flinging ended, new symbol: ${currentSymbol.name}")
                
                // 새 카드가 들어오는 애니메이션
                val newCardAnim = ObjectAnimator.ofPropertyValuesHolder(
                    cardView,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 0.1f, 1f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.1f, 1f)
                )
                newCardAnim.duration = 300L
                newCardAnim.interpolator = DecelerateInterpolator()
                
                newCardAnim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        isCardFlinging = false
                        
                        // 카드 날아간 후에는 새로운 문양으로 멈춰있는 상태 유지
                        // 필요하면 여기서 새로 애니메이션 시작 가능
                        Log.d(TAG, "Card returned with new symbol - ready for interaction")
                    }
                })
                
                newCardAnim.start()
            }
        })
        
        animator.start()
    }
    
    /**
     * 카드를 원래 위치로 돌려놓는 애니메이션
     */
    private fun returnCardToOriginalPosition() {
        // 현재 실행 중인 애니메이션 중지 (상태는 유지)
        forceStopAllAnimations()
        
        val returnAnim = ObjectAnimator.ofPropertyValuesHolder(
            cardView,
            PropertyValuesHolder.ofFloat(View.X, cardView.x, initialX),
            PropertyValuesHolder.ofFloat(View.Y, cardView.y, initialY),
            PropertyValuesHolder.ofFloat(View.ROTATION, cardView.rotation, 0f)
        )
        
        returnAnim.duration = 200
        returnAnim.interpolator = DecelerateInterpolator()
        
        returnAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // 카드가 원래 위치로 돌아온 후 상태 갱신
                Log.d(TAG, "Card returned to original position")
                // 카드가 원래 위치로 돌아온 후에는 애니메이션 없이 정지 상태로 유지
            }
        })
        
        returnAnim.start()
    }
    
    /**
     * 클릭 이벤트 처리
     */
    override fun performClick(): Boolean {
        Log.d(TAG, "performClick called")
        // 다른 클릭 처리 방식과 일관되게 forceToggleAnimation 사용
        if (!isCardFlinging && !isDragging) {
            forceToggleAnimation()
        }
        return super.performClick()
    }
    
    /**
     * 다음 문양으로 변경
     */
    fun changeToNextSymbol() {
        currentSymbol = currentSymbol.next()
        updateCardSymbol()
    }
    
    /**
     * 이전 문양으로 변경
     */
    fun changeToPreviousSymbol() {
        currentSymbol = when (currentSymbol) {
            CardSymbolType.SPADE -> CardSymbolType.CLUB
            CardSymbolType.HEART -> CardSymbolType.SPADE
            CardSymbolType.DIAMOND -> CardSymbolType.HEART
            CardSymbolType.CLUB -> CardSymbolType.DIAMOND
        }
        updateCardSymbol()
    }
    
    /**
     * 카드 문양 업데이트
     */
    private fun updateCardSymbol() {
        val (symbol, color) = when (currentSymbol) {
            CardSymbolType.SPADE -> Pair("♠", Color.BLACK)
            CardSymbolType.HEART -> Pair("♥", Color.RED)
            CardSymbolType.DIAMOND -> Pair("♦", Color.RED)
            CardSymbolType.CLUB -> Pair("♣", Color.BLACK)
        }
        
        // 중앙 큰 문양 업데이트
        cardSymbolCenter.text = symbol
        cardSymbolCenter.setTextColor(color)
        
        // 왼쪽 상단 문양 업데이트
        cardSymbolTopLeft.text = symbol
        cardSymbolTopLeft.setTextColor(color)
        
        // 오른쪽 하단 문양 업데이트
        cardSymbolBottomRight.text = symbol
        cardSymbolBottomRight.setTextColor(color)
        
        Log.d(TAG, "Card symbol updated to: ${currentSymbol.name}")
    }
    
    /**
     * 애니메이션 설정
     */
    fun setCardAnimation(animation: Animation) {
        try {
            // 애니메이션 설정 및 속성 지정
            animation.repeatCount = Animation.INFINITE
            animation.repeatMode = Animation.RESTART
            animation.fillAfter = true
            animation.fillBefore = true
            animation.isFillEnabled = true
            currentAnimation = animation
            Log.d(TAG, "Card animation set successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting animation: ${e.message}")
        }
    }
    
    /**
     * 애니메이션 시작
     */
    fun startCardAnimation(animation: Animation) {
        try {
            Log.d(TAG, "Start card animation called, animation: ${animation}, isFlinging: ${isCardFlinging}")
            
            // 카드가 날아가는 중이면 애니메이션 시작하지 않음
            if (isCardFlinging) {
                Log.d(TAG, "Skip animation start - card is flinging")
                return
            }
            
            // 기존 애니메이션이 있으면 제거
            cardView.clearAnimation()
            
            // 애니메이션 무한 반복 설정 확인
            if (animation.repeatCount != Animation.INFINITE) {
                animation.repeatCount = Animation.INFINITE
                animation.repeatMode = Animation.RESTART
                animation.fillAfter = true
                animation.fillBefore = true
                animation.isFillEnabled = true
            }
            
            // 새 애니메이션 설정 및 시작
            currentAnimation = animation
            
            // 애니메이션을 전체 카드 뷰에 적용
            cardView.startAnimation(animation)
            isAnimationPlaying = true
            
            Log.d(TAG, "Card animation started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting animation: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * 애니메이션 중지
     */
    fun stopAnimation() {
        try {
            Log.d(TAG, "Stop animation called")
            
            // Animation 중지
            cardView.clearAnimation()
            
            // ObjectAnimator도 중지 
            cardView.animate().cancel()
            
            // rotationAnimator가 있으면 명시적으로 중지
            rotationAnimator?.let {
                Log.d(TAG, "Cancelling rotation animator")
                it.cancel()
                it.removeAllListeners()
                rotationAnimator = null
            }
            
            // View의 모든 애니메이션 및 애니메이터 중지
            cardView.clearAnimation()
            cardView.animation?.cancel()
            
            // 추가로 자식 뷰들의 애니메이션도 중지
            if (cardView is ViewGroup) {
                for (i in 0 until cardView.childCount) {
                    cardView.getChildAt(i).clearAnimation()
                }
            }
            
            // 회전 초기화
            cardView.rotation = 0f
            cardView.rotationY = 0f
            cardView.rotationX = 0f
            
            // 모든 애니메이션 객체 정리
            cardView.animate().cancel()
            cardView.clearAnimation()
            
            // 애니메이션 상태 초기화
            isAnimationPlaying = false
            
            Log.d(TAG, "Card animation stopped completely")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping animation: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * 강제 애니메이션 토글 - 상태에 관계 없이 명확하게 전환
     */
    private fun forceToggleAnimation() {
        Log.d(TAG, "Force toggle animation called, current state: $isAnimationPlaying")
        
        try {
            // 현재 애니메이션이 돌고 있는지 실제로 확인
            val isActuallyAnimating = (cardView.animation != null) || (rotationAnimator?.isRunning == true)
            Log.d(TAG, "Actual animation state: $isActuallyAnimating, tracked state: $isAnimationPlaying")
            
            if (isActuallyAnimating || isAnimationPlaying) {
                // 애니메이션이 실제로 재생 중이거나 상태가 재생 중으로 표시된 경우
                Log.d(TAG, "Force stopping all animations")
                forceStopAllAnimations()
            } else {
                // 카드가 정지 상태일 때는 2바퀴 회전 애니메이션 시작
                Log.d(TAG, "Force starting 2-rotation animation")
                forceStartAnimation()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in forceToggleAnimation: ${e.message}")
            e.printStackTrace()
            
            // 오류가 발생하면 모든 애니메이션 초기화
            forceStopAllAnimations()
        }
    }
    
    /**
     * 모든 애니메이션 강제 중지
     */
    private fun forceStopAllAnimations() {
        try {
            Log.d(TAG, "Force stopping all animations")
            
            // 1. 기존 Animation 객체 중지
            cardView.clearAnimation()
            if (cardView.animation != null) {
                cardView.animation.cancel()
            }
            
            // 2. 모든 ObjectAnimator 취소
            rotationAnimator?.cancel()
            rotationAnimator?.removeAllListeners()
            rotationAnimator = null
            
            // 3. View의 animate() 메서드로 생성된 애니메이션 취소
            cardView.animate().cancel()
            
            // 4. 모든 자식 뷰의 애니메이션 취소
            if (cardView is ViewGroup) {
                for (i in 0 until cardView.childCount) {
                    val child = cardView.getChildAt(i)
                    child.clearAnimation()
                    child.animate().cancel()
                }
            }
            
            // 5. 모든 회전 값 초기화
            cardView.rotation = 0f
            cardView.rotationY = 0f
            cardView.rotationX = 0f
            
            // 6. 확실하게 상태 업데이트
            isAnimationPlaying = false
            
            Log.d(TAG, "All animations forcibly stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error in forceStopAllAnimations: ${e.message}")
            e.printStackTrace()
            // 상태만이라도 업데이트
            isAnimationPlaying = false
        }
    }
    
    /**
     * 애니메이션 강제 시작 - 2바퀴만 회전하고 멈춤
     */
    private fun forceStartAnimation() {
        try {
            Log.d(TAG, "Force starting animation (2 rotations)")
            
            // 먼저 모든 애니메이션 중지하고 상태 초기화
            forceStopAllAnimations()
            
            // 새 회전 애니메이션 생성
            val rotation = ObjectAnimator.ofFloat(cardView, View.ROTATION_Y, 0f, 720f) // 720도 = 2바퀴
            rotation.duration = 3000 // 3초 동안 2바퀴 회전
            rotation.repeatCount = 0 // 반복 없음 - 한 번만 실행
            rotation.interpolator = DecelerateInterpolator()
            
            // 멤버 변수에 설정
            rotationAnimator = rotation
            
            // 명확한 상태 설정
            isAnimationPlaying = true
            
            // 리스너 추가
            rotation.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) {
                    super.onAnimationCancel(animation)
                    Log.d(TAG, "Rotation animation was cancelled")
                    isAnimationPlaying = false
                }
                
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    Log.d(TAG, "2 rotations completed, animation ended")
                    // 애니메이션이 끝나면 상태 업데이트
                    isAnimationPlaying = false
                    // 회전 초기화
                    cardView.rotationY = 0f
                }
            })
            
            // 애니메이션 시작
            rotation.start()
            
            Log.d(TAG, "Animation started: 2 rotations (720 degrees)")
        } catch (e: Exception) {
            Log.e(TAG, "Error in forceStartAnimation: ${e.message}")
            e.printStackTrace()
            isAnimationPlaying = false
        }
    }
    
    /**
     * 애니메이션 토글
     */
    fun toggleAnimation() {
        // 기존 로직을 forceToggleAnimation으로 위임
        forceToggleAnimation()
    }
    
    /**
     * 카드 애니메이션 테스트 - 문제 해결용
     * 외부에서 직접 호출하여 애니메이션 강제 시작
     */
    fun testCardAnimation() {
        Log.d(TAG, "Test card animation called")
        forceStopAllAnimations() // 기존 애니메이션 정지
        forceStartAnimation()
    }
    
    /**
     * 현재 애니메이션 상태 반환
     */
    fun isAnimationActive(): Boolean = isAnimationPlaying
    
    /**
     * 애니메이션 상태 설정
     */
    fun setAnimationActive(active: Boolean) {
        isAnimationPlaying = active
    }
} 