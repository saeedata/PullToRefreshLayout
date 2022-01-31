package com.saeed.pulltorefreshlayout

import android.content.res.Resources


fun Int.toDp():Int = this.toDpf().toInt()
fun Int.toPx():Int = this.toPxf().toInt()
fun Int.toDpf():Float = this.toFloat().toDpf()
fun Int.toPxf():Float = this.toFloat().toPxf()
fun Float.toDp():Int = this.toDpf().toInt()
fun Float.toPx():Int = this.toDpf().toInt()
fun Float.toDpf():Float = this * Resources.getSystem().displayMetrics.density
fun Float.toPxf():Float = this / Resources.getSystem().displayMetrics.density