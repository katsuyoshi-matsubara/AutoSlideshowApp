package jp.techacademy.katsuyoshi.matsubara.autoslideshowapp

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private val PERMISSIONS_REQUEST_CODE = 100
    private var mTimer: Timer? = null
    private var mTimerSec = 0.0
    private var mHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 許可されている
                getContentsInfo()
            } else {
                // 許可されていないので許可ダイアログを表示する
                requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSIONS_REQUEST_CODE
                )
            }
            // Android 5系以下の場合
        } else {
            getContentsInfo()
        }

        playButton.setOnClickListener {
            if (mTimer == null) {
                mTimer = Timer()
                if (playButton.text == "再生") {
                    mTimer!!.schedule(object : TimerTask() {
                        override fun run() {
                            mTimerSec = (mTimerSec + 0.1) % 2
                            //mTimerSecを2で割った余り→1.9の後リセット
                            //リセットされないタイマーと並べて2.0秒と確認済み
                            mHandler.post {
                                timer.text = String.format("%.1f", mTimerSec)
                            }
                        }
                    }, 100, 100)
                    playButton.text = "停止"
                }
            } else {//if(mTimer != null)は、上のif文(mTimer==null) のelseなので抜いた。
                //"停止"でカウントをリセットしてしまう
                mTimerSec = 0.0 //これも次の行のもないとおかしくなる
                timer.text = "0.0"
                mTimer!!.cancel()
                mTimer = null
                playButton.text = "再生"
            }



            forwardButton.setOnClickListener {
                getContentsInfo()


            }
            backButton.setOnClickListener {

            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getContentsInfo()
                }
        }
    }

    private fun getContentsInfo() {
        // 画像の情報を取得する
        val resolver = contentResolver //ContentResolverはContentProviderのデータを参照するためのクラス
        val cursor = resolver.query( //ContentResolverクラスのqueryメソッドを使って条件を指定して検索
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // 第1引数データの種類
            null, // 項目(null = 全項目)
            null, // フィルタ条件(null = フィルタなし)
            null, // フィルタ用パラメータ
            null // ソート (null ソートなし)
        ) //結果はCursorクラス、Cursorとはデータベース上の検索結果を格納するもの

        if (cursor!!.moveToFirst()) {
            //moveToFirstメソッドで先頭を指し、moveToNextメソッドで順番に移動
            //どちらのメソッドも情報があり指すことができたらtrue無ければfalseを返す
            //indexからIDを取得し、そのIDから画像のURIを取得する

                val fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                //cursorが指しているデータの中から画像のIDがセットされている位置を取得
                val id = cursor.getLong(fieldIndex)//cursor.getLong()で画像のIDを取得
                val imageUri =
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                //ContentUris.withAppendedId()でそこから実際の画像のURIを取得
                imageView.setImageURI(imageUri)
            //Log.d("ANDROID", "URI : " + imageUri.toString())
            // } (do) while(cursor.moveToNext())で表示
        }
        cursor.close()
    }
}