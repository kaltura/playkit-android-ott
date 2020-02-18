package com.kaltura.playkit.mediaproviders;

import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.playkit.PKMediaEntry;

import com.kaltura.playkit.providers.base.OnMediaLoadCompletion;
import com.kaltura.playkit.providers.mock.MockMediaProvider;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.kaltura.playkit.providers.MediaProvidersUtils.buildNotFoundlErrorElement;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by tehilarozin on 09/11/2016.
 */

@RunWith(AndroidJUnit4.class)
public class MockMediaProviderAndroidTest {

    final static String InputFile = "mock/entries.playkit.json";

    public MockMediaProviderAndroidTest() {
    }

    @Test
    public void testMockProvider() {

        final MockMediaProvider mockMediaProvider = new MockMediaProvider(InputFile, InstrumentationRegistry.getTargetContext(), "dash");
        mockMediaProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                if (response.isSuccess()) {
                    PKMediaEntry mediaEntry = response.getResponse();
                    System.out.println("got some response. id = " + mediaEntry.getId());
                } else {
                    assertFalse(response.getError() == null);
                    System.out.println("got error on json load: " + response.getError().getMessage());
                }

                mockMediaProvider.id("1_1h1vsv3z").load(new OnMediaLoadCompletion() {
                    @Override
                    public void onComplete(ResultElement<PKMediaEntry> response) {
                        assertTrue(response.isSuccess());
                        assertTrue(response.getError() == null);
                        PKMediaEntry mediaEntry = response.getResponse();
                        assertTrue(mediaEntry.getId().equals("1_1h1vsv3z"));
                        assertTrue(mediaEntry.getSources().get(0).getId().equals("1_ude4l5pb"));

                        mockMediaProvider.id("notexists").load(new OnMediaLoadCompletion() {
                            @Override
                            public void onComplete(ResultElement<PKMediaEntry> response) {
                                assertTrue(!response.isSuccess());
                                assertTrue(response.getError() != null);
                                assertTrue(response.getError().equals(buildNotFoundlErrorElement("can't find media")));
                            }
                        });
                    }
                });

            }
        });
    }
}
