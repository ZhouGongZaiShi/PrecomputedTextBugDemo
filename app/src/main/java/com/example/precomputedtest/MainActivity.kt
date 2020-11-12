package com.example.precomputedtest

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.*
import android.text.style.BackgroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.PrecomputedTextCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.lang.ref.Reference
import java.lang.ref.WeakReference
import java.util.concurrent.Executor
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fromHtml = SpannableString("aaaabbgbb").apply {
            setSpan(MyUnder(), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(MyBg(Color.RED), 2, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StrikethroughSpan(), 3, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        findViewById<AppCompatTextView>(R.id.text).apply {
//            setText(fromHtml)
            Text((fromHtml))
        }

        findViewById<RecyclerView>(R.id.rv).run {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = MyAdapter()
        }
    }

    class MyUnder : UnderlineSpan() {
        override fun getSpanTypeId(): Int {
            Log.d("MyUnder", "[zhouxin] getSpanTypeId ")
            return super.getSpanTypeId()
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        override fun updateDrawState(ds: TextPaint) {
            Log.d("MyUnder", "[zhouxin] updateDrawState ${ds}")
            super.updateDrawState(ds)
        }
    }

    class MyBg(color: Int) : BackgroundColorSpan(color) {
        override fun getSpanTypeId(): Int {
            Log.d("MyBg", "[zhouxin] getSpanTypeId ")
            return super.getSpanTypeId()
        }

        override fun updateDrawState(ds: TextPaint) {
            Log.d("MyBg", "[zhouxin] updateDrawState ${ds.isUnderlineText}")
            super.updateDrawState(ds)
        }
    }

    fun newSpannableString(charSequence: CharSequence): SpannableString {
        val clz = SpannableString::class.java
        val declaredConstructor =
            clz.getDeclaredConstructor(CharSequence::class.java, Boolean::class.javaPrimitiveType)

        Log.d("MainActivity", "[zhouxin] newSpannableString $declaredConstructor")
        return declaredConstructor.newInstance(charSequence, true)
    }


    class MyAdapter : RecyclerView.Adapter<MyVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyVH {
            val view = AppCompatTextView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            return MyVH(view)
        }

        override fun onBindViewHolder(holder: MyVH, position: Int) {
            val charSequence = Html.fromHtml("<div><strike><u>aaaabbgbb</u></strike></div>")
            if (position % 2 == 0) {
//                holder.textView.Text(charSequence)
                asyncSetText(holder.textView, charSequence, UI_PRECOMPUTE_EXECUTOR)
            } else {
                holder.textView.text = (charSequence)
            }
        }

        override fun getItemCount(): Int {
            return 10
        }
    }

    class MyVH(itemView: AppCompatTextView) : RecyclerView.ViewHolder(itemView) {
        val textView = itemView
    }
}

val UI_PRECOMPUTE_EXECUTOR = Executors.newFixedThreadPool(2)

@RequiresApi(Build.VERSION_CODES.P)
fun AppCompatTextView.Text(charSequence: CharSequence) {
    val content = charSequence
    // In particular, when using SetTextFuture(), attributes such as typeface, textDirection, text content and text size must be set before use,
    // otherwise an exception will occur when the API is below 28.
    setTextFuture(
        PrecomputedTextCompat.getTextFuture(
            content,
            TextViewCompat.getTextMetricsParams(this),
            UI_PRECOMPUTE_EXECUTOR
        )
    )
}

// from https://developer.android.google.cn/reference/android/text/PrecomputedText?hl=en
@RequiresApi(Build.VERSION_CODES.P)
fun asyncSetText(textView: TextView, longString: CharSequence, bgExecutor: Executor) {
    // construct precompute related parameters using the TextView that we will set the text on.
    val params = textView.textMetricsParams
    val textViewRef: Reference<TextView> = WeakReference(textView)
    bgExecutor.execute {
        val textView: TextView = textViewRef.get()?: return@execute
        val precomputedText = PrecomputedText.create(longString, params)
        textView.post {
            val textView: TextView = textViewRef.get() ?: return@post
            textView.text = precomputedText
        }
    }
}