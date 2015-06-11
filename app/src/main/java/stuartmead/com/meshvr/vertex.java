package stuartmead.com.meshvr;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.google.vrtoolkit.cardboard.EyeTransform;
import com.google.vrtoolkit.cardboard.HeadTransform;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by smead on 8/23/2014.
 */
public class vertex {

    private static final String TAG = "VertexAct";

    private final String vertexShaderCode =
            "uniform mat4 u_MVPMatrix;\n"+
                    "attribute vec4 vPosition;\n" +
                    "attribute vec4 color;\n" +
                    "varying vec4 fragColor;\n" +
                    "void main()\n {\n" +
                    "gl_PointSize = 5.0;\n" +
                    "gl_Position = u_MVPMatrix\n" +
                    "* vPosition;\n" +
                    "fragColor = color;\n" +
                    "}\n";

    private final String strFShader =
            "precision mediump float;" +
                    "varying vec4 fragColor;" +
                    "void main() {" +
                    "  gl_FragColor = fragColor;" +
                    "}";

    private FloatBuffer vertexBuffer;
    static final int COORDS_PER_VERTEX=3;
    static final int COLS_PER_VERTEX=4;
    private final int vertexProgram;
    private int vertexPositionHandle;
    private int vertexColorHandle;

    private static final float TIME_DELTA = 1.0001f;
    private float zFactor = 1.0f;
    private static final float CAMERA_Z = 2.5f;

    private int vertexMVPHandle;

    private float[] mMVPMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mCameraMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private float[] mModelMatrix = new float[16];
    private float[] mHeadView = new float[16];

    private float[] mHeadTrans = new float[3];
    private float[] mViewVector = new float[3];
    /** How many elements per vertex. */
    private final int vertexStride = (COORDS_PER_VERTEX+COLS_PER_VERTEX) * 4;//4 bytes per float

    /** Offset of the position data. */
    private final int vertexPositionOffset = 0;

    /** Offset of the color data. */
    private final int vertexColorOffset = COORDS_PER_VERTEX;


    /*static float vertexCoords[]={
            0.72575943f,	0.408092172f	,	0.572121346f	,
            0.173878637f,	0.871991926f	,	0.555411357f	,
            0.7963666f	,	0.027353724f	,	0.138071501f	,
            0.874914373f	,	0.655158172f	,	0.245985066f	,
            0.501759872f,	0.978337188f	,	0.034014057f	,
            0.304233571f	,	0.558071341f	,	0.649339082f	,
            0.956814171f	,	0.143322086f	,	0.376209699f	,
            0.318822989f	,	0.991085681f	,	0.919923402f	,
            0.672777726f	,	0.481898782f	,	0.033859866f	,
            0.949897708f	,	0.572684382f	,	0.679266555f	,
            0.910922083f	,	0.759049747f	,	0.5468084f	,
            0.386877876f	,	0.61644713f	,	0.589121802f	,
            0.311087448f	,	0.364030331f	,	0.10576982f	,
            0.483295015f	,	0.077740584f	,	0.760841189f	,
            0.506371981f,	0.643178762f	,	0.442439803f	,
            0.406516474f	,	0.033738569f	,	0.781269393f	,
            0.268671231f	,	0.32877006f	,	0.844790777f	,
            0.568914915f	,	0.597929024f	,	0.538382116f	,
            0.030914727f	,	0.118341368f	,	0.117642614f	,
            0.33807366f	,	0.580505221f	,	0.410549579f
            /*0.0f,  0.622008459f, 0.0f,   // top
            -0.5f, -0.311004243f, 0.0f,   // bottom left
            0.5f, -0.311004243f, 0.0f    // bottom right
    };

    private final int vertexCount = vertexCoords.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * 4;*/

    //float color[] = {0.865532921f	,	0.271003948f	,	0.733093116f	,	1.0f};

    static float rgbVertex[] = {
            0.00614567f, 0.286434415f, 0.277559509f,
            0.278203789f, 0.82504246f, 0.184875315f, 1.0f,
            0.748944971f, 0.177491826f, 0.696642833f,
            0.88447768f, 0.091378204f, 0.874995871f, 1.0f,
            0.083185344f, 0.079460643f, 0.037449963f,
            0.275927619f, 0.929562008f, 0.981081918f, 1.0f,
            0.18222268f, 0.179828862f, 0.06246036f,
            0.607465678f, 0.208728882f, 0.300400694f, 1.0f,
            0.061122968f, 0.471975209f, 0.606803975f,
            0.477174292f, 0.079864615f, 0.262359766f, 1.0f,
            0.834922874f, 0.676366383f, 0.005933008f,
            0.255545562f, 0.994970864f, 0.009094976f, 1.0f,
            0.91942798f, 0.212509044f, 0.927223654f,
            0.872037088f, 0.70742586f, 0.351313365f, 1.0f,
            0.597158843f, 0.121894529f, 0.456180013f,
            0.916918173f, 0.42607923f, 0.659015128f, 1.0f,
            0.37661072f, 0.101698138f, 0.245116271f,
            0.904412802f, 0.691230509f, 0.440888927f, 1.0f,
            0.368920137f, 0.857450378f, 0.157352744f,
            0.066094922f, 0.030327436f, 0.734508741f, 1.0f,
            0.797440547f, 0.203834311f, 0.62627237f,
            0.824987826f, 0.481163343f, 0.943997012f, 1.0f,
            0.234715917f, 0.264459512f, 0.900101164f,
            0.443281684f, 0.955499039f, 0.879159707f, 1.0f,
            0.085543216f, 0.863468855f, 0.950199967f,
            0.909634492f, 0.111811347f, 0.497734486f, 1.0f,
            0.356685167f, 0.304902919f, 0.974280397f,
            0.01416136f, 0.868121688f, 0.025973311f, 1.0f,
            0.57576459f, 0.232163124f, 0.801742644f,
            0.92862823f, 0.960759805f, 0.384226752f, 1.0f,
            0.595684121f, 0.21870984f, 0.135416997f,
            0.750626658f, 0.979862911f, 0.723889582f, 1.0f,
            0.39616895f, 0.803980187f, 0.492723896f,
            0.465307178f, 0.860620509f, 0.088700485f, 1.0f,
            0.299617772f, 0.632409099f, 0.164712581f,
            0.71730675f, 0.015765957f, 0.215288023f, 1.0f,
            0.97320856f, 0.598055927f, 0.904259333f,
            0.073808346f, 0.452402221f, 0.971984089f, 1.0f
    };

    private int vertexCount;

    float[]vertices;


    public vertex(float[] vertices){

        if (vertices == null){
            vertices = rgbVertex;
            Log.i(TAG, "Vertices is null, using rgbVertex");
        }
            Log.i(TAG,"Vertices are x"+vertices[0]+"y"+vertices[1]
                    +"z"+vertices[2]+"r"+vertices[3]+"g"+vertices[4]+"b"+vertices[5]+"a"+vertices[6]);
        vertexCount = vertices.length / (COORDS_PER_VERTEX+COLS_PER_VERTEX);
        //init vertex bytebuffer
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                vertices.length * 4);
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());

        // create a floating point buffer from the ByteBuffer
        vertexBuffer = bb.asFloatBuffer();
        // add the coordinates to the FloatBuffer
        vertexBuffer.put(vertices);
        // set the buffer to read the first coordinate
        vertexBuffer.position(0);


        int vertexShader = MainActivity.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = MainActivity.loadShader(GLES20.GL_FRAGMENT_SHADER, strFShader);

        vertexProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(vertexProgram, vertexShader);
        GLES20.glAttachShader(vertexProgram, fragmentShader);

        GLES20.glBindAttribLocation(vertexProgram, 0, "vPosition");
        GLES20.glBindAttribLocation(vertexProgram, 1, "color");

        GLES20.glLinkProgram(vertexProgram);
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 20f ,-20f, -40f);
    }

    public void preparetoDraw(HeadTransform hT){
        GLES20.glUseProgram(vertexProgram);
        vertexPositionHandle = GLES20.glGetAttribLocation(vertexProgram, "vPosition");
        vertexColorHandle = GLES20.glGetAttribLocation(vertexProgram, "color");
        vertexMVPHandle = GLES20.glGetUniformLocation(vertexProgram, "u_MVPMatrix");

        hT.getHeadView(mHeadView,0);
        //Matrix.rotateM(mModelMatrix, 0, TIME_DELTA, 0.5f, 0.5f, 1.0f);
        if (this.getzScale())
        {
            //zFactor = zFactor + (zFactor * TIME_DELTA);
            hT.getForwardVector(mViewVector,0);
            hT.getTranslation(mHeadTrans,0);
            if (mViewVector[2] > 0)
            {

               Log.i(TAG, "Negative Y");
               /*if (mViewVector[2] < 0){
                   Log.i(TAG, "Negative Z");
               }*/

               Matrix.translateM(mModelMatrix,0,mViewVector[0]*TIME_DELTA,-mViewVector[1]*TIME_DELTA,
                        -mViewVector[2]*TIME_DELTA);
            }else {
                Matrix.translateM(mModelMatrix, 0, mViewVector[0] * TIME_DELTA, mViewVector[1] * TIME_DELTA,
                        -mViewVector[2] * TIME_DELTA);
            }
           //Matrix.scaleM(mModelMatrix,0,zFactor,zFactor,zFactor);
        }

        Matrix.setLookAtM(mCameraMatrix, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);



    }

    public void draw(EyeTransform trans){//float[] viewMat, float[] modelMat, float[] projMat

        vertexPositionHandle = GLES20.glGetAttribLocation(vertexProgram, "vPosition");
        vertexColorHandle = GLES20.glGetAttribLocation(vertexProgram, "color");
        vertexMVPHandle = GLES20.glGetUniformLocation(vertexProgram, "u_MVPMatrix");


        GLES20.glUseProgram(vertexProgram);

        vertexBuffer.position(vertexPositionOffset);
        GLES20.glVertexAttribPointer(vertexPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(vertexPositionHandle);

        //Now pass in color info
        vertexBuffer.position(COORDS_PER_VERTEX);
        GLES20.glVertexAttribPointer(vertexColorHandle, COLS_PER_VERTEX,
                GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
        GLES20.glEnableVertexAttribArray(vertexColorHandle);

        //Apply eye transform to camera
        Matrix.multiplyMM(mViewMatrix, 0, trans.getEyeView(),0,mCameraMatrix,0);

        //View mat by model mat
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);


        //Get near and far clip planes
        trans.getParams().getFov().toPerspectiveMatrix(0.001f,1000.0f,mProjectionMatrix,0);

        //Model mat by projemat
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);//trans.getPerspective()

        GLES20.glUniformMatrix4fv(vertexMVPHandle, 1, false, mMVPMatrix, 0);


        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, vertexCount);


        // Disable vertex array
        //GLES20.glDisableVertexAttribArray(vertexPositionHandle);
    }

    private boolean zScale;

    public boolean getzScale() {return zScale;}
    public void setzScale(boolean zS) {zScale = zS;}

    public void setVertices (float[] v){vertices = v;}
}

