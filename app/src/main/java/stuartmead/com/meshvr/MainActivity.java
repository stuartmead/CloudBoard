package stuartmead.com.meshvr;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.opengl.GLES20;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.EyeTransform;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.microedition.khronos.egl.EGLConfig;


public class MainActivity extends CardboardActivity implements CardboardView.StereoRenderer {

    private vertex mVertex;
    float[] vertices;
    private static final String TAG = "MainActivity";

    /** Thread executor for generating cube data in the background. */
    private final ExecutorService mSingleThreadedExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void setVolumeKeysMode(int mode) {
        super.setVolumeKeysMode(VolumeKeys.DISABLED);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        CardboardView cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);

        Intent intent = getIntent();
        String action = intent.getAction();


        if (Intent.ACTION_VIEW.equals(action)){
            Toast toast = Toast.makeText(getApplicationContext(), "Loading:"+intent.getDataString(), Toast.LENGTH_LONG);
            toast.show();
            //new DownloadFilesTask().execute(intent.getData());
            //handleUri(intent.getData());
            cardboardView.queueEvent(new OpenFile(intent.getData()));

        }

        // mOverlayView = (CardboardOverlayView) findViewById(R.id.overlay);
    }


    protected void handleUri(Uri uri){
        if (uri !=null){
            if (uri.getScheme().equals("file")){
                //TODO: Convert the following to an AsyncTask
                //new DownloadFilesTask().execute(uri);
                Log.i(TAG, "Executing DownloadFilesTask");
                try {
                    Log.i(TAG, "Reading file"+uri.getPath());
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                    DataInputStream di = new DataInputStream(new FileInputStream(uri.getPath()));

                    String str;
                    br.readLine();//Ply
                    br.readLine();//Format
                    br.readLine();//Comment
                    str = br.readLine();// element vertex 99014
                    String[]vertentries = str.split(" ");
                    int numVertices = Integer.parseInt(vertentries[2]);
                    Log.i(TAG, "Number of vertices:"+numVertices);
                    br.readLine();//x
                    br.readLine();//y
                    br.readLine();//z
                    br.readLine();//red
                    br.readLine();//green
                    br.readLine();//blue
                    br.readLine();//alpha
                    str = br.readLine();//end_header OR element face
                    int numElems = 0;
                    if (!str.equals("end_header"))//Wont do anything with them atm
                    {
                        String[]elemEntries = str.split(" ");
                        numElems = Integer.parseInt(elemEntries[2]);
                        Log.i(TAG, "Number of elems:"+numElems);
                        br.readLine();//property list uchar int vertex index
                        br.readLine();//end_header;
                    }

                    //Now read binary (only read binary)
                    int l = 0;
                    while(!di.readLine().equals("end_header")){
                        ++l;
                    };
                    /*float readLittleFloat(DataInputStream d)throws IOException{
                        return Float.intBitsToFloat(readLittleInt(d));
                    }*/

                    /*int readLittleInt(DataInputStream d) throws IOException{
                        d.readFully(byteBuffer,0,4);
                        return (byteBuffer[3]) << 24 | (byteBuffer[2] & 0xff) << 16 |
                                (byteBuffer[1] & 0xff) << 8 | (byteBuffer[0] & 0xff);
                    }*/
                    Log.i(TAG, "Skipped lines"+l);
                    int entriesCounter = 0;
                    vertices = new float[numVertices*7];
                    for (int i = 0; i < numVertices; ++i)
                    {
                        float x, y, z, r, g, b, a;
                        byte byteBuffer[] = new byte[4];
                        di.readFully(byteBuffer,0,4);
                        x=Float.intBitsToFloat((byteBuffer[3]) << 24 | (byteBuffer[2] & 0xff) << 16 |
                                (byteBuffer[1] & 0xff) << 8 | (byteBuffer[0] & 0xff));
                        vertices[entriesCounter] = x;
                        ++entriesCounter;
                        di.readFully(byteBuffer,0,4);
                        y=Float.intBitsToFloat((byteBuffer[3]) << 24 | (byteBuffer[2] & 0xff) << 16 |
                                (byteBuffer[1] & 0xff) << 8 | (byteBuffer[0] & 0xff));
                        vertices[entriesCounter] = y;
                        ++entriesCounter;
                        di.readFully(byteBuffer,0,4);
                        z=Float.intBitsToFloat((byteBuffer[3]) << 24 | (byteBuffer[2] & 0xff) << 16 |
                                (byteBuffer[1] & 0xff) << 8 | (byteBuffer[0] & 0xff));
                        vertices[entriesCounter] = z;
                        ++entriesCounter;
                        r = (float) di.readUnsignedByte()/255;
                        vertices[entriesCounter] = r;
                        ++entriesCounter;
                        g = (float) di.readUnsignedByte()/255;
                        vertices[entriesCounter] = g;
                        ++entriesCounter;
                        b = (float) di.readUnsignedByte()/255;
                        vertices[entriesCounter] = b;
                        ++entriesCounter;
                        a = (float) di.readUnsignedByte()/255;
                        vertices[entriesCounter] = a;
                        ++entriesCounter;
                        if (i == 0){
                            Log.i(TAG,"x"+x+"y"+y+"z"+z+"r"+r+"g"+g+"b"+b+"a"+a);
                        }
                    }
                    di.close();
                    //To do: faces


                }catch (FileNotFoundException fnfe){
                    Toast toast = Toast.makeText(getApplicationContext(), "Cannot open: File Not Found", Toast.LENGTH_LONG);
                    toast.show();
                }catch (IOException e){
                    Toast toast = Toast.makeText(getApplicationContext(), "Cannot open file: IO error", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        Log.i(TAG, "onNewFrame");
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        mVertex.preparetoDraw(headTransform);
    }

    @Override
    public void onDrawEye(EyeTransform eyeTransform) {
        Log.i(TAG, "onDrawEye");
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        mVertex.draw(eyeTransform);
        //mVertex.draw(eyeTransform);
    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        // GLES20.glViewport(0, 0, width, height);
        Log.i(TAG, "onSurfaceChanged");
    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {
        if (vertices != null) {
            Log.i(TAG, "onSurfaceCreated, vertices size is" + vertices.length);
        } else
        {
            Log.i(TAG, "onSurfaceCreated, vertices size is null");
        }
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        mVertex = new vertex(vertices);
        mVertex.setzScale(false);

    }

    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }
    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    @Override
    public void onCardboardTrigger() {
        super.onCardboardTrigger();
        if (mVertex.getzScale())
        {
            mVertex.setzScale(false);
            //mVibrator.vibrate(50);
            //mVibrator.vibrate(50);
        }
        else
        {
            mVertex.setzScale(true);
            //mVibrator.vibrate(50);
            Log.i(TAG, "Scaling true");
        }
    }

    class OpenFile implements Runnable {
        Uri RunUri;

        OpenFile(Uri uri){
            RunUri = uri;
        }
        @Override
        public void run() {
            handleUri(RunUri);
        }
    }
}

