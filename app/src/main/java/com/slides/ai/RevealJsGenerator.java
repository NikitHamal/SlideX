package com.slides.ai;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class RevealJsGenerator {

    private Context context;

    public RevealJsGenerator(Context context) {
        this.context = context;
    }

    public String generateHtml(List<String> slideHtmlList) {
        String revealJs = readRawResource(R.raw.reveal);
        String revealCss = readRawResource(R.raw.reveal_css);
        String themeCss = readRawResource(R.raw.black_css);
        String fontCss = readRawResource(R.raw.source_sans_pro);

        StringBuilder slidesHtml = new StringBuilder();
        for (String slideHtml : slideHtmlList) {
            slidesHtml.append(slideHtml);
        }

        return "<!doctype html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta charset=\"utf-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n" +
                "  <title>reveal.js</title>\n" +
                "  <style>\n" +
                "    " + fontCss + "\n" +
                "  </style>\n" +
                "  <style>\n" +
                "    " + revealCss + "\n" +
                "  </style>\n" +
                "  <style>\n" +
                "    " + themeCss + "\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"reveal\">\n" +
                "    <div class=\"slides\">\n" +
                "      " + slidesHtml.toString() + "\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  <script>\n" +
                "    " + revealJs + "\n" +
                "  </script>\n" +
                "  <script>\n" +
                "    Reveal.initialize({\n" +
                "      embedded: true,\n" +
                "      width: 320,\n" +
                "      height: 200,\n" +
                "      margin: 0,\n" +
                "      minScale: 1,\n" +
                "      maxScale: 1\n" +
                "    });\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";
    }

    private String readRawResource(int resourceId) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            InputStream inputStream = context.getResources().openRawResource(resourceId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            reader.close();
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }
}
