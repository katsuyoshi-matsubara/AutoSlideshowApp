package jp.techacademy.katsuyoshi.matsubara.autoslideshowapp

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {
    //URIを格納する配列を作成
    private var playingCondition = 0 //0:停止中 1:再生中
    private var imageUris: Array<Uri?> = arrayOfNulls(64) //最初にサイズを決めないことできる？
    private var uriNumber = 0 // URI配列のインデックス
    private var uriNumberMax = 0 //URI配列の最後のインデックス
    private val PERMISSIONS_REQUEST_CODE = 100
    private var mTimer: Timer? = null //??
    private var mTimerSec = 0.0
    private var mHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("ANDROID", "main $playingCondition")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                getContentsInfo()  // 許可されている
            } else {
                requestPermissions(  // 許可されていないので許可ダイアログを表示する
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSIONS_REQUEST_CODE
                )
            }
        } else {  // Android 5系以下の場合
            getContentsInfo()
        }

        playButton.setOnClickListener {
            if (mTimer == null) {
                mTimer = Timer()
                if (playingCondition == 0) {
                    mTimer!!.schedule(object : TimerTask() {
                        override fun run() {
                            mTimerSec = (mTimerSec + 0.1) % 2
                            //mTimerSecを2で割った余り→1.9の後リセット
                            //リセットされないタイマーと並べて2.0秒と確認済み
                            mHandler.post {
                                timer.text = String.format("%.1f", mTimerSec)
                                /*UI の描画は、メインスレッド（UI Thread とも呼ばれます）でのみ可能
                                そのため、サブスレッドはメインスレッドに描画を依頼する形になります。
                                描画の依頼は mHandler.post() で行います。
                                mHandler.post() 内の処理は UI 描画なのでメインスレッドに依頼する必要
                                Handler はスレッドを超えて依頼をするために使用します。*/
                                if (String.format("%.1f", mTimerSec) == "0.0") { //mTimerSec == 0.0ではダメ
                                    if (imageUris[uriNumber + 1] != null) {
                                        //Log.d("ANDROID", "$uriNumber")
                                        uriNumber ++  //次があれば次、なければ最初へ
                                        imageView.setImageURI(imageUris[uriNumber])
                                    } else {
                                        //Log.d("ANDROID", "else$uriNumber")
                                        uriNumber = 0
                                        imageView.setImageURI(imageUris[uriNumber])
                                    }
                                }
                            }
                        }
                    }, 100, 100)
                    playButton.text = "停止"
                    playingCondition = 1
                }
            } else {//if(mTimer != null)は、上のif文(mTimer==null) のelseなので抜いた。
                //"停止"でカウントをリセット
                mTimerSec = 0.0 //これも次の行のもないとおかしくなる
                timer.text = String.format("%.1f", mTimerSec)
                mTimer!!.cancel()
                mTimer = null
                playButton.text = "再生"
                playingCondition = 0
            }
        }
        //最初反応せずに再生を押してから反応していたのはplayButtonの｛｝の中にほかのボタンが入ってた
        forwardButton.setOnClickListener {
            Log.d("ANDROID", "Condition = $playingCondition")
            if (playingCondition == 0) {
                if (imageUris[uriNumber + 1] != null) {
                    uriNumber++
                    imageView.setImageURI(imageUris[uriNumber])
                } else {
                    uriNumber = 0
                    imageView.setImageURI(imageUris[uriNumber])
                }
            }
        }

        backButton.setOnClickListener {
            if (playingCondition == 0) {
                if (uriNumber != 0) {
                    uriNumber--
                    imageView.setImageURI(imageUris[uriNumber])
                } else {
                    uriNumber = uriNumberMax
                    imageView.setImageURI(imageUris[uriNumber])
                }
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
            do {
                val fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                //cursorが指しているデータの中から画像のIDがセットされている位置を取得
                val id = cursor.getLong(fieldIndex)//cursor.getLong()で画像のIDを取得
                val imageUri =
                    ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                //ContentUris.withAppendedId()でそこから実際の画像のURIを取得
                Log.d("ANDROID", "URI : $imageUri")
                try {
                    imageUris[uriNumber] = imageUri
                    uriNumber++
                } catch (e: Exception) {
                }
            } while (cursor.moveToNext())

            // } (do) while(cursor.moveToNext())で存在する分だけ表示
            fileNumber.text = "$uriNumber files" //ファイル数を表示
            uriNumberMax = uriNumber - 1 //imageUrisの最後のindexを保持→backButtonで使用
            uriNumber = 0 // 上のサイクルで増加したuriNumberをリセット（やらないとplayButtonでelseから入る）
            imageView.setImageURI(imageUris[0])
        }
        Log.d("ANDROID", "getcontentsinfo $playingCondition")
        cursor.close()
    }
}