package com.saeed.pulltorefreshlayout

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val list = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")

        val mAdapter = SimpleAdapter(list)
        val rvList = findViewById<RecyclerView>(R.id.rv_list).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = mAdapter
        }

        val prtList = findViewById<PullToRefreshLayout>(R.id.ptr_list).apply {
            setRefreshListener(object : PullToRefreshLayout.OnRefreshListener {
                override fun onRefresh() {
                    postDelayed({
                        this@apply.setRefreshing(isRefreshing = false, isAnimating = true)
                    }, 5000)

                }

                override fun onRefreshViewAnimatingStatusChanged(isAnimating: Boolean) {

                }

            })
        }
    }
}

class SimpleAdapter(private val list: List<String>) : RecyclerView.Adapter<SimpleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleViewHolder {
        return SimpleViewHolder(SimpleItemCell(parent.context))
    }

    override fun onBindViewHolder(holder: SimpleViewHolder, position: Int) {
        holder.itemView.findViewById<TextView>(android.R.id.text1).text = list[position]
    }

    override fun getItemCount(): Int {
        return list.size
    }

}


class SimpleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class SimpleItemCell @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = -1,
    defStyleRes: Int = -1,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    init {
        LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, this, true)
    }
}
