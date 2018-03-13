package com.trick2live.rtf_parser;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;


import com.trick2live.parser.rtf.exception.PlainTextExtractorException;
import com.trick2live.parser.rtf.exception.UnsupportedMimeTypeException;
import com.trick2live.parser.rtf.parser.PlainTextExtractor;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView textView = findViewById(R.id.text);
        String demo = "{\\rtf1\\ansi\\deff0\n" +
                "{\\colortbl;\\red0\\green0\\blue0;\\red255\\green0\\blue0;}\n" +
                "This line is the default color\\line\n" +
                "\\cf2\n" +
                "This line is red\\line\n" +
                "\\cf1\n" +
                "This line is the default color\n" +
                "}";

        InputStream stream = new ByteArrayInputStream(demo.getBytes(StandardCharsets.UTF_8));
        PlainTextExtractor extractor = new PlainTextExtractor();
        try {

            String text = extractor.extract(
                    new BufferedInputStream(stream),
                    "application/rtf"
            );
            textView.setText(text);
        } catch (UnsupportedMimeTypeException e) {
            e.printStackTrace();
        } catch (PlainTextExtractorException e) {
            e.printStackTrace();
        }
    }
}
