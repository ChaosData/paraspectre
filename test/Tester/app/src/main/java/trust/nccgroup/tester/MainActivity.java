package trust.nccgroup.tester;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.widget.RelativeLayout;
import android.widget.TextView;

import trust.nccgroup.tester.util.ViewGroupUtils;

public class MainActivity extends Activity {
    private static final String TAG = "NCC/Tester/MainActivity";

    private String concat(String s, int i) {
        Log.e(TAG, "called concat(\"" + s + "\", " + i + ")");
        return s + i;
    }

    private static class FooView extends TextView {

        public FooView(Context context) {
            super(context);
        }

        @Override
        public CharSequence getText() {
            Log.e(TAG, "called getText()");
            return "Foo!";
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tv = (TextView)findViewById(R.id.textView);
        tv.setText("Hello!");

        TextView tv2 = (TextView)findViewById(R.id.textView2);

        FooView foo = new FooView(this.getApplicationContext());
        foo.setText(foo.getText());
        foo.setLayoutParams(tv2.getLayoutParams());

        ViewGroupUtils.replaceView(tv2, foo);

        Log.e(TAG, concat("foo", 55));
    }
}
