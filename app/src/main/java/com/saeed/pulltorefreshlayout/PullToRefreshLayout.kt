package com.saeed.pulltorefreshlayout

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

@SuppressLint("ClickableViewAccessibility")
class PullToRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = -1,
    defStyleRes : Int = -1,
) : FrameLayout(context, attrs, defStyleAttr,defStyleRes), View.OnTouchListener {

    interface OnRefreshListener {
        fun onRefresh()
        fun onRefreshViewAnimatingStatusChanged(isAnimating: Boolean)
    }

    private var targetListView: RecyclerView? = null
    private var progressView: PullToRefreshProgressView
    private var backToTopAnimation: ObjectAnimator? = null
    private var pulledDownY = 0
    private var lastTouchY = -1
    private val maxPullDownY: Float
    private val pullToRefreshThreshold: Float
    private var didPassRefreshThreshold = false
    private var allowPullToRefresh = false
    private var isRefreshTriggered = false
    private var maybeStartTracking = false
    private var forceEndTouchSession = false
    private var startedTrackingY = 0
    private val progressViewSize: Int
    private var onRefreshListener: OnRefreshListener? = null
    private var onListTouchListener: OnTouchListener? = null

    init {
        clipChildren = false
        clipToPadding = false
        progressView = PullToRefreshProgressView(context)
        progressViewSize = 33
        addView(
            progressView,
            LayoutParams(progressViewSize.toDp(),progressViewSize.toDp(), Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
                topMargin = 19.toDp()
            }
        )
        pullToRefreshThreshold = 80.toDpf()
        maxPullDownY = pullToRefreshThreshold * 3f
        progressView.setOnTouchListener(this@PullToRefreshLayout)
        progressView.isClickable = true
        progressView.isFocusable = true
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        if (childCount == 0) {
            return
        }
        if (targetListView == null) {
            ensureTarget()
        }
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (targetListView == null) {
            ensureTarget()
        }
        progressView.measure(
            MeasureSpec.makeMeasureSpec(progressViewSize.toDp(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(progressViewSize.toDp(), MeasureSpec.EXACTLY)
        )
    }

    private fun ensureTarget() {
        if (targetListView == null) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child != progressView) {
                    bindListViewToLayout(child as RecyclerView?)
                    break
                }
            }
        }
    }

    private fun bindListViewToLayout(targetListView: RecyclerView?) {
        if (this.targetListView != null) return
        this.targetListView = targetListView
        this.targetListView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {}
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                checkForPullAllowance(targetListView)
            }
        })
        updateListTop()
        this.targetListView?.setOnTouchListener(this)
    }

    private fun checkForPullAllowance(listView: RecyclerView?) {
        if (listView == null) return
        val layoutManager: LinearLayoutManager = listView.layoutManager as LinearLayoutManager
        allowPullToRefresh = layoutManager.findFirstCompletelyVisibleItemPosition() == 0
    }

    private fun updateListTop() {
        if (backToTopAnimation == null && pulledDownY >= 0) {
            val progress = pulledDownY / pullToRefreshThreshold
            if (progress==0f){
                progressView.isVisible = false
            }else{
                progressView.isVisible = true
                progressView.setProgress(progress)
            }
            targetListView?.translationY = pulledDownY.toFloat()
        }
    }

    @SuppressLint("ObjectAnimatorBinding")
    private fun changePullToRefreshMode(animating: Boolean) {
        val animateToY = if (didPassRefreshThreshold) pullToRefreshThreshold else 0f
        if (animateToY != pulledDownY.toFloat()) {
            if (animating) {
                backToTopAnimation =
                    ObjectAnimator.ofFloat(targetListView, "translationY", animateToY)
                backToTopAnimation?.duration = 200
                if (targetListView?.isComputingLayout == false) {
                    targetListView?.suppressLayout(true)
                    onRefreshListener?.onRefreshViewAnimatingStatusChanged(true)
                    backToTopAnimation?.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            backToTopAnimation = null
                            settlePullToRefreshView()
                            targetListView?.suppressLayout(false)
                            onRefreshListener?.onRefreshViewAnimatingStatusChanged(false)
                            super.onAnimationEnd(animation)
                        }
                    })
                    backToTopAnimation?.start()
                } else {
                    backToTopAnimation = null
                    settlePullToRefreshView()
                    onRefreshListener?.onRefreshViewAnimatingStatusChanged(false)
                }
            } else {
                settlePullToRefreshView()
            }
        }
    }

    private fun settlePullToRefreshView() {
        if (!didPassRefreshThreshold || forceEndTouchSession) {
            if (forceEndTouchSession) {
                forceEndTouchSession = false
                targetListView?.setOnTouchListener(this)
            }
            progressView.resetState()
            pulledDownY = 0
        } else {
            pulledDownY = pullToRefreshThreshold.toInt()
        }
        updateListTop()
        if (pulledDownY == 0) {
            isRefreshTriggered = false
        } else {
            triggerRefreshCallback()
        }
    }

    private fun triggerRefreshCallback() {
        if (onRefreshListener != null && !isRefreshTriggered) {
            onRefreshListener?.onRefresh()
            isRefreshTriggered = true
        }
    }

    override fun onTouch(v: View, e: MotionEvent): Boolean {
        val action: Int = e.action
        onListTouchListener?.onTouch(v, e)
        if (action == MotionEvent.ACTION_MOVE) {
            if (forceEndTouchSession) {
                endOfTouchSession()
                return super.onTouchEvent(e)
            }
            if (backToTopAnimation == null) {
                if (maybeStartTracking) {
                    var deltaY = abs(startedTrackingY - e.rawY).toInt()
                    val rawDeltaY = (startedTrackingY - e.rawY).toInt()
                    val isScrollingDown: Boolean = lastTouchY < e.rawY
                    lastTouchY = e.rawY.toInt()
                    if (abs(deltaY) > 1) {
                        if (didPassRefreshThreshold) {
                            if (rawDeltaY <= 0) deltaY += pullToRefreshThreshold.toInt() else {
                                deltaY = (pullToRefreshThreshold - deltaY).toInt()
                            }
                        }
                        val preventScrolling =
                            !isScrollingDown && (pulledDownY <= 0 || rawDeltaY > 0 && pulledDownY - rawDeltaY <= 0 && !didPassRefreshThreshold)
                        return if (!preventScrolling) {
                            if (deltaY > maxPullDownY) {
                                deltaY = maxPullDownY.toInt()
                            }
                            pulledDownY = deltaY
                            if (pulledDownY >= pullToRefreshThreshold) {
                                triggerRefreshCallback()
                            }
                            updateListTop()
                            true
                        } else {
                            endOfTouchSession()
                            super.onTouchEvent(e)
                        }
                    }
                } else if (allowPullToRefresh) {
                    maybeStartTracking = true
                    startedTrackingY = e.rawY.toInt()
                    lastTouchY = startedTrackingY
                } else {
                    endOfTouchSession()
                }
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            endOfTouchSession()
        }
        return super.onTouchEvent(e)
    }

    private fun endOfTouchSession() {
        lastTouchY = -1
        startedTrackingY = 0
        maybeStartTracking = false
        if (forceEndTouchSession) {
            targetListView?.setOnTouchListener(null)
        } else if (pulledDownY != 0) {
            didPassRefreshThreshold = pulledDownY >= pullToRefreshThreshold
        }
        changePullToRefreshMode(true)
    }

    fun cancelAnimation() {
        backToTopAnimation?.cancel()
        backToTopAnimation = null
    }

    fun isRefreshing(): Boolean = didPassRefreshThreshold || isRefreshTriggered

    fun isInteracted(): Boolean = maybeStartTracking || didPassRefreshThreshold || pulledDownY > 0 || backToTopAnimation != null

    fun setRefreshing(isRefreshing: Boolean, isAnimating: Boolean) {
        didPassRefreshThreshold = isRefreshing
        ensureTarget()
        if (!maybeStartTracking) {
            changePullToRefreshMode(isAnimating)
        } else{
            forceEndTouchSession = true
        }
    }

    fun setRefreshListener(onRefreshListener: OnRefreshListener?) {
        this.onRefreshListener = onRefreshListener
    }

    fun setOnListViewTouchListener(onListTouchListener: OnTouchListener?) {
        this.onListTouchListener = onListTouchListener
    }

    fun setUserInteractionEnabled(userInteractionEnabled: Boolean) {
        if (userInteractionEnabled) {
            targetListView?.setOnTouchListener(this@PullToRefreshLayout)
            progressView.setOnTouchListener(this@PullToRefreshLayout)
        } else {
            targetListView?.setOnTouchListener(null)
            progressView.setOnTouchListener(null)
        }
    }

}