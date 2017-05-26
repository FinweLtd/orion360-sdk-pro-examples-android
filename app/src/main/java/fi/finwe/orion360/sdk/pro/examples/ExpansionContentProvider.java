/*
 * Copyright (c) 2016, Finwe Ltd. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package fi.finwe.orion360.sdk.pro.examples;

import com.android.vending.expansion.zipfile.APEZProvider;

/**
 * APEZProvider enables URI access to the files in the app's expansion package (.obb file).
 * <p/>
 * This is useful e.g. for playing videos that are embedded to the app using an expansion package:
 * There is no need to extract a copy of the video, as Android MediaPlayer is capable for
 * decoding video straight from the expansion package via URI access given by an APEZProvider,
 * provided that it was added to the .obb file without compressing it.
 * <p/>
 * The main reason for using an (optional) expansion package is that it allows publishing
 * an app whose installation files are larger than 100 MB in the Google Play store. In addition,
 * the possibility to update the app without forcing users to download all content again
 * is valuable, especially for apps that come bundled with large video files that rarely (if ever)
 * need to be updated.
 * <p/>
 * To use the APEZProvider, you need to install 'Google Play APK Expansion Library' with
 * Android SDK Manager, and then find the library project under your SDK installation path:
 *     [sdk]/extras/google/google_market_apk_expansion/zip_file
 * For your convenience, the compiled library is included to this project under the /libs folder.
 */
public class ExpansionContentProvider extends APEZProvider {

    /** Authority string for the content provider. */
    public static final String AUTHORITY =
            "fi.finwe.orion360.sdk.pro.examples.ExpansionContentProvider";

    @Override
    public String getAuthority() {

        // The authority string must match with the one defined in the app's manifest file.
        return AUTHORITY;

    }

}
