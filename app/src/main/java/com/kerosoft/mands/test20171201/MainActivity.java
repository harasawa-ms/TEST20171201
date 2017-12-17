package com.kerosoft.mands.test20171201;

import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter mNfcAdapter;

    //デバッグメッセージ用タグ
    private static final String TAG = "DB_TEST";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //NFCを扱うためのインスタンスを取得
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // open db
        UserOpenHelper userOpenHelper = new UserOpenHelper(this);
        SQLiteDatabase db = userOpenHelper.getWritableDatabase();

        //transaction
        try{
            db.beginTransaction();
            db.execSQL(
                  "update users " +
                  "set score = score + 15 " +
                  "where name = 'taguchi' "
            );

            db.execSQL(
                    "update users " +
                            "set score = score - 20 " +
                            "where name = 'fkoji' "
            );

            //commit
            db.setTransactionSuccessful();

        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            db.endTransaction();
        }
        ;





        Cursor c = null;

        c = db.rawQuery(
                " select * from users where score >= 10 ",null
        );


        Log.v("DB_TEST","COUNT:"+c.getCount());

        while(c.moveToNext()){
            int id = c.getInt(c.getColumnIndex("_id"));
            String name = c.getString(c.getColumnIndex("name"));
            int score = c.getInt(c.getColumnIndex("score"));
            Log.v("DB_TEST","id: " + id + " name:" + name + " score:" + score);
        }
        c.close();

        //close db
        db.close();

    }


    @Override
    protected void onResume() {
        super.onResume();

        //NFCカードがかざされた際に、現在のアクティビティで優先的に受け取る設定を行う
        Intent intent = new Intent(this,this.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

        //第１引数：Activity
        //第２引数：Intent
        //第３引数：IntentFilter…nullにすることで全対象
        //第４引数：TechLists…nullにすることで全対象
        mNfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);


    }

    @Override
    protected void onPause() {
        super.onPause();

        //Activityがバックグラウンドに回った際は、優先的に受け取る情報を停止する
        mNfcAdapter.disableForegroundDispatch(this);



   }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // NFC-UIDを取得する
        byte[] uid = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
        TextView txt05 = (TextView)findViewById(R.id.text05);

        //NFC-UIDを文字列に変換して表示する
        //Toast.makeText(this, Arrays.toString(uid),Toast.LENGTH_LONG).show();
        TextView txt01 = (TextView)findViewById(R.id.text02);
        txt01.setText(Arrays.toString(uid));


        //
        StringBuilder tagId = new StringBuilder();

        for (int i=0;i<uid.length;i++){
            tagId.append(String.format("%02x",uid[i]));

        }

        TextView txt03 = (TextView)findViewById(R.id.text03);
        String IDmstr = tagId.toString();
        txt03.setText(IDmstr);

        // open db
        UserOpenHelper userOpenHelper = new UserOpenHelper(this);
        SQLiteDatabase db = userOpenHelper.getWritableDatabase();
        Cursor c = null;

        String sqlstr;



        sqlstr = "insert into users(name,score,IDm) values" +
                "(" +
                "'hara'," +
                "38," +
                "'" +
                IDmstr +
                "'" +
                ")";

        Log.d("DB_TEST",sqlstr);

        //インサート
        db.execSQL(sqlstr);

        c = db.rawQuery(
                " select * from users ",null
        );

        Log.v("DB_TEST","COUNT:"+c.getCount());

        // カードID取得。Activityはカード認識時起動に設定しているのでここで取れる。
        byte[] felicaIDm = new byte[]{0};

        //タグ取得
        Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            //IDm:製造ID取得
            felicaIDm = tag.getId();
            Toast.makeText(this, felicaIDm.toString(), Toast.LENGTH_SHORT).show();
        }

        NfcF nfc = NfcF.get(tag);
        try {

            //接続
            nfc.connect();

            //コマンドパケット送信
            byte[] req = readWithoutEncryption(felicaIDm,10);

            //コマンドパケット★デバッグ★
            Log.d(TAG, "req:"+toHex(req));

            // コマンドパケット送信→リクエストパケット受信
            byte[] res = nfc.transceive(req);

            String strmsg = "[" + res.length + "byte:" + res.toString() +"]";
            Toast.makeText(this, strmsg, Toast.LENGTH_LONG).show();

            //レスポンスパケット★デバッグ★
            Log.d(TAG, "res:"+toHex(res));

            //切断
            nfc.close();

            // レスポンスパケットからデータを取得→表示

            txt05.setText(parse(res));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage() , e);
            txt05.setText(e.toString());
        }


        while(c.moveToNext()){
            int id = c.getInt(c.getColumnIndex("_id"));
            String name = c.getString(c.getColumnIndex("name"));
            int score = c.getInt(c.getColumnIndex("score"));
            String IDm = c.getString(c.getColumnIndex("IDm"));
            Log.v("DB_TEST","id: " + id + " name:" + name + " score:" + score + " IDm" + IDm);
        }
        c.close();


    }


    /**
     * Felicaコマンドの取得。
     * - Sonyの「FeliCa Lite-Sユーザーズマニュアル  」の仕様から。
     * - http://www.sony.co.jp/Products/felica/business/tech-support/index.html
     *
     * @param idm カードのID
     * @return Felicaコマンド
     */
    private byte[] readWithoutEncryption(byte[] idm,int size)
            throws IOException {

        //コマンド組み立て用のバイト配列
        ByteArrayOutputStream bout = new ByteArrayOutputStream(100);

        bout.write(0x99);        // [0]  データ長ダミー（最後に、生成したデータ長で上書き）
        bout.write(0x06);        // [1] Felicaコマンド「Read Without Encryption」
        bout.write(idm);            //  [2～9] カードID 8byte
        bout.write(1);          //  [10] サービス数
        bout.write(0x0f);       //  [11]サービスコード下位バイト（リトルエンディアン　履歴サービスコード0x090f）
        bout.write(0x09);       //  [12]サービスコード上位バイト（リトルエンディアン　履歴サービスコード0x090f）
        bout.write(size);           //  [13]ブロック数

        for (int i=0; i < size;i++){
            bout.write(0x80);
            bout.write(i);
        }

        byte[] msg = bout.toByteArray();
        msg[0] = (byte) msg.length; // 先頭１バイトにデータ長をセット
        return msg;

    }



    /**
     * Felica応答の解析。
     * @param res Felica応答
     * @return 文字列表現
     * @throws Exception
     */
    private String parse(byte[] res) throws Exception {

        // res[0] : データ長
        // res[1] : 0x07

        // res[2～9] : カードID
        // res[10,11] : エラーコード。0=正常
        if (res[10] != 0x00){
            throw new RuntimeException("Felica error.");
        }

        // res[12] : 応答ブロック数
        // res[13 + n*16] : 履歴データ。16byteブロックの繰り返し

        //社員番号の桁数
        int size = res[12];
        String str = "";

        for (int i = 0 ;i < size ; i++){
            //個々の履歴の解析
            Rireki rireki = Rireki.parse(res,13 + i * 16);
        }

        //レスポンスパケットのデータ開始位置
        int startDigits = 13;

        //社員番号取り出し用の配列を定義
        //byte[] res2 = new byte[numDigits];

        int cnt=0;
        String tmp = "";

        //レスポンスパケットから、ブロックデータ部分を取得（13byte～19byte）

        return tmp;
    }

    //デバッグ用にbyte(-128～127)→0x00形式へ変換
    private String toHex(byte[] id) {
        StringBuilder sbuf = new StringBuilder();
        for (int i = 0; i < id.length; i++) {
            String hex = "0" + Integer.toString((int) id[i] & 0x0ff, 16);
            if (hex.length() > 2)
                hex = hex.substring(1, 3);
            sbuf.append(" " + i + ":" + hex);
        }
        return sbuf.toString();
    }



}
