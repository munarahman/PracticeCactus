package com.practicecactus.practicecactus.Cactus;

import android.widget.ImageView;

import com.practicecactus.practicecactus.R;

/**
 * Created by matthew on 2016-03-05.
 */
public class CactusView {

    ImageView image;

    public CactusView(ImageView cactusHolder) {
        this.image = cactusHolder;
    }

    public void setMood(float mood) {
        int imageID;
        if (mood < 0.1) {
            imageID = R.drawable.cactus_1;
        } else if (mood < 0.2) {
            imageID = R.drawable.cactus_2;
        } else  if (mood < 0.3) {
            imageID = R.drawable.cactus_3;
        } else  if (mood < 0.4) {
            imageID = R.drawable.cactus_4;
        } else  if (mood < 0.6) {
            imageID = R.drawable.cactus_5;
        } else  if (mood < 0.7) {
            imageID = R.drawable.cactus_6;
        } else  if (mood < 0.8) {
            imageID = R.drawable.cactus_7;
        } else  if (mood < 0.9) {
            imageID = R.drawable.cactus_8;
        } else {
            imageID = R.drawable.cactus_9;
        }
        this.image.setImageResource(imageID);
    }
}
