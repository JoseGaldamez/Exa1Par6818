package dev.josegaldamez.exa1par6818.views;

import android.os.Bundle;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

import dev.josegaldamez.exa1par6818.R;

public class MembersActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_members);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.membersRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        MaterialToolbar toolbar = findViewById(R.id.membersToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;
        ((ImageView) findViewById(R.id.membersLogo)).setImageBitmap(
                BitmapFactory.decodeResource(getResources(), R.drawable.uth_logo, options));
    }
}
