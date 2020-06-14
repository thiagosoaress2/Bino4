package com.bino.bino1.Utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class dateMask(var editText: EditText, protected var mMask: String) : TextWatcher {


    private var isUpdating: Boolean = false
    protected var mOldString = ""
    internal var befores = ""

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        befores = s.toString().replace("[^\\d]".toRegex(), "")

    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        var str = s.toString().replace("[^\\d]".toRegex(), "")

        if (str.length == 0) {
            return
        }

        if (before == 1 && befores.length > 0 && !isUpdating) {
            val last = befores.substring(befores.length, befores.length)
            val rep = last.replace("(", "").replace(")", "").replace(" ", "").replace("-", "")
            if (rep.length == 0) {
                str = str.substring(0, befores.length - 1)
            }
        }


        val mask = StringBuilder()
        if (isUpdating) {
            mOldString = str
            isUpdating = false
            return
        }
        var i = 0
        for (m in mMask.toCharArray()) {
            if (m != '#') {
                mask.append(m)
                continue
            }
            try {
                mask.append(str[i])
            } catch (e: Exception) {
                break
            }

            i++
        }
        isUpdating = true
        val x = mask.toString()
        editText.setText(x)
        editText.setSelection(mask.length)

    }

    override fun afterTextChanged(s: Editable) {
    }
}