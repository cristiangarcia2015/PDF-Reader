package com.example.pdfreader

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.graphics.pdf.PdfRenderer
import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Environment
import android.widget.Toast
import android.os.ParcelFileDescriptor
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    var path: String = Environment.getExternalStorageDirectory().absolutePath + "/demo.pdf"
    var pageIndex: Int = 0
    var pdfRenderer: PdfRenderer? = null
    var curPage: PdfRenderer.Page? = null
    var descriptor: ParcelFileDescriptor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //set click listener on buttons
        btnPrevious.setOnClickListener{
            //get the index of previous page
            if (curPage!=null) {
                val index = curPage!!.index - 1
                displayPage(index)
            }
        }

        btnNext.setOnClickListener{
            //get the index of previous page
            if (curPage!=null) {
                val index = curPage!!.index + 1
                displayPage(index)
            }
        }

        startDownload()
    }

    private fun startDownload() {
        try {
            progress.visibility= View.VISIBLE
         DownloadTask(path){
            openPdfRenderer()
            displayPage(pageIndex)
             progress.visibility=View.GONE
        }.execute()
        } catch (e: Exception) {
            Toast.makeText(this, "Sorry! This pdf is protected with password.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        try {
            closePdfRenderer()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    private fun openPdfRenderer() {
        val file = File(path)
        descriptor = null
        pdfRenderer = null
        try {
            descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(descriptor!!)
        } catch (e: Exception) {
            Toast.makeText(this, "There's some error", Toast.LENGTH_LONG).show()
        }
    }

    private fun closePdfRenderer() {
        if (curPage != null) curPage!!.close()
        if (pdfRenderer != null) pdfRenderer!!.close()
        if (descriptor != null) descriptor!!.close()
    }

    private fun displayPage(index: Int) {
        if (pdfRenderer!!.pageCount <= index)
            return
        //close the current page
        if (curPage != null) curPage!!.close()
        //open the specified page
        curPage = pdfRenderer!!.openPage(index)
        //get page width in points(1/72")
        val pageWidth = curPage!!.width
        //get page height in points(1/72")
        val pageHeight = curPage!!.height
        //returns a mutable bitmap
        val bitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888)
        //render the page on bitmap
        curPage!!.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        //display the bitmap
        imgView.setImageBitmap(bitmap)
        //enable or disable the button accordingly
        val pageCount = pdfRenderer!!.pageCount
        btnPrevious.isEnabled = 0 != index
        btnNext.isEnabled = index + 1 < pageCount
    }

    private open class DownloadTask(private val path:String?, private val onFinish:()->Unit) : AsyncTask<String, Integer, String>() {

        override fun doInBackground(vararg params: String?): String {
            var input:InputStream? = null
            var output: OutputStream? = null
            var connection: HttpURLConnection? = null
            try {
                val url =  URL("http://www.pdf995.com/samples/pdf.pdf")
                connection = url.openConnection() as HttpURLConnection
                connection.connect()

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.responseCode + " " + connection.responseMessage
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                val fileLength = connection.contentLength

                // download the file
                input = connection.inputStream
                output = FileOutputStream(path)

                val data = ByteArray(1024)
                var total = 0
                var count=0
                do {
                    count = input.read(data)
                    // allow canceling with back button
                    if (isCancelled) {
                        input.close()
                        return ""
                    }
                    total += count
                    output.write(data, 0, count)
                }
                while (count != -1)

            } catch ( e:Exception) {
                return e.toString()
            } finally {
                try {
                        output?.close()
                        input?.close()
                } catch ( ignored:IOException) {
                }

                    connection?.disconnect()
            }
            return ""
        }

        override fun onPostExecute(result: String?) {
            onFinish()
        }
        }
}
