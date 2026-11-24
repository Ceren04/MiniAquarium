package com.miniakvaryum.game.core.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

/** Test 11 of the plan "Konten und Profil": adjusting the photo in the circle. */
class PhotoFrameTest {

    @Test
    void atTheStartThePhotoIsCenteredAndFillsTheCircle() {
        PhotoFrame frame = new PhotoFrame(2000, 1000, 400);
        assertThat(frame.scale()).isEqualTo(0.4f);
        assertThat(frame.shownHeight()).isEqualTo(400f);
        assertThat(frame.crop()).isEqualTo(new PhotoFrame.Crop(500, 0, 1000));
    }

    @Test
    void cannotBeMadeSmallerThanTheCircle() {
        PhotoFrame frame = new PhotoFrame(2000, 1000, 400);
        frame.zoom(0.5f);
        assertThat(frame.scale()).isEqualTo(0.4f);
    }

    @Test
    void zoomStopsAtFourTimes() {
        PhotoFrame frame = new PhotoFrame(1000, 1000, 400);
        for (int i = 0; i < 10; i++) {
            frame.zoom(1.5f);
        }
        assertThat(frame.scale()).isCloseTo(1.6f, Offset.offset(1e-5f));
        assertThat(frame.crop().size()).isEqualTo(250);
    }

    @Test
    void cannotBeMovedOutOfTheCircle() {
        PhotoFrame frame = new PhotoFrame(2000, 1000, 400);
        frame.move(10_000, 10_000);
        // Shown 800 x 400: 200 px of room to each side, none up or down.
        assertThat(frame.offsetX()).isEqualTo(200f);
        assertThat(frame.offsetY()).isZero();
        // Photo moved right: the circle shows the left end of the photo.
        assertThat(frame.crop()).isEqualTo(new PhotoFrame.Crop(0, 0, 1000));
        frame.move(-10_000, 0);
        assertThat(frame.crop()).isEqualTo(new PhotoFrame.Crop(1000, 0, 1000));
    }

    @Test
    void zoomedCropFollowsTheMove() {
        PhotoFrame frame = new PhotoFrame(1000, 1000, 400);
        frame.zoom(2f);
        // Shown 800 x 800, circle sees 500 x 500. Move the photo down by 100 screen px:
        // the circle looks 125 photo px higher up (y down in the photo).
        frame.move(0, -100);
        assertThat(frame.crop()).isEqualTo(new PhotoFrame.Crop(250, 125, 500));
        frame.move(0, -1_000);
        assertThat(frame.crop()).isEqualTo(new PhotoFrame.Crop(250, 0, 500));
    }

    @Test
    void zoomKeepsTheSameSpotInTheCenter() {
        PhotoFrame frame = new PhotoFrame(1000, 1000, 400);
        frame.zoom(2f);
        frame.move(100, 0);
        frame.zoom(1.5f);
        assertThat(frame.offsetX()).isCloseTo(150f, Offset.offset(1e-3f));
    }

    @Test
    void needsASize() {
        assertThatThrownBy(() -> new PhotoFrame(0, 10, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PhotoFrame(10, 10, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
