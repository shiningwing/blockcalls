package com.jachness.blockcalls.services;

import android.annotation.SuppressLint;
import android.preference.PreferenceManager;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.jachness.blockcalls.AndroidTest;
import com.jachness.blockcalls.entities.BlackListNumberEntity;
import com.jachness.blockcalls.exceptions.PhoneNumberException;
import com.jachness.blockcalls.exceptions.TooShortNumberException;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

/**
 * Created by jachness on 12/11/2016.
 */
@SuppressWarnings({"TryWithIdenticalCatches", "ConstantConditions"})
@RunWith(AndroidJUnit4.class)
public final class MatcherServiceTest extends AndroidTest {
    private static final String TAG = MatcherServiceTest.class.getSimpleName();
    @Inject

    MatcherService matcherService;
    @Inject

    NormalizerService normalizerService;

    @SuppressLint("CommitPrefEdits")
    @Before
    public void setUp() throws Exception {
        super.setUp();
        PreferenceManager.getDefaultSharedPreferences(getTargetContext()).edit().clear().commit();
        getComponent().inject(this);
    }

    @Test
    public void isPhoneNumberMatchTest() {

        String callerID, defaultRegion, otherNumber;

        callerID = "1148556045";
        defaultRegion = "AR";
        otherNumber = "4855-6045";
        Assert.assertTrue(isMatch(callerID, defaultRegion, otherNumber));

        //6 digits
        callerID = "3837426789";
        defaultRegion = "AR";
        otherNumber = "426-789";
        Assert.assertTrue(isMatch(callerID, defaultRegion, otherNumber));

        callerID = "3837426789";
        defaultRegion = "AR";
        otherNumber = "26-789";
        Assert.assertTrue(isMatch(callerID, defaultRegion, otherNumber));

        callerID = "3837426789";
        defaultRegion = "AR";
        otherNumber = "6-789";
        Assert.assertTrue(isMatch(callerID, defaultRegion, otherNumber));


        callerID = "3837-426789";
        defaultRegion = "AR";
        otherNumber = "+543837-426789";
        Assert.assertTrue(isMatch(callerID, defaultRegion, otherNumber));


        //Mobiles with prefix 15 (argentina) always match for non strict matching
        callerID = "11-5491-4754";
        defaultRegion = "AR";
        otherNumber = "15-5491-4754";
        Assert.assertTrue(isMatch(callerID, defaultRegion, otherNumber));

        callerID = "";
        defaultRegion = "zz";
        otherNumber = "+";
        Assert.assertFalse(isMatch(callerID, defaultRegion, otherNumber));

        callerID = null;
        defaultRegion = null;
        otherNumber = null;
        Assert.assertFalse(isMatch(callerID, defaultRegion, otherNumber));

        callerID = "011231234567";
        defaultRegion = "US";
        otherNumber = "01 123 7654321";
        Assert.assertFalse(isMatch(callerID, defaultRegion, otherNumber));

        //Brazilian mobiles lacks of area code if parties are in the same area
        callerID = "21034117";
        defaultRegion = "BR";
        otherNumber = "(21) 2103-4117";
        Assert.assertTrue(isMatch(callerID, defaultRegion, otherNumber));

        //
        callerID = "2103-4117";
        defaultRegion = "BR";
        otherNumber = "+55 (21) 2103-4117";
        Assert.assertTrue(isMatch(callerID, defaultRegion, otherNumber));

        //
        callerID = "2103-4117";
        defaultRegion = "BR";
        otherNumber = "+55 2103-4117";
        Assert.assertTrue(isMatch(callerID, defaultRegion, otherNumber));

    }

    private boolean isMatch(String callerID, String defaultRegion, String otherNumber) {
        boolean res = false;
        try {
            Call call = new Call();
            call.setNumber(callerID);
            call.setCountryISO(defaultRegion);
            normalizerService.normalizeCall(call);
            BlackListNumberEntity entity = new BlackListNumberEntity();
            entity.setNormalizedNumber(otherNumber);
            res = matcherService.isNumberMatch(call, entity);
            Log.i(TAG, callerID + " | " + defaultRegion + " | " + otherNumber + " | " + " | [" +
                    res + "]");

        } catch (PhoneNumberException e) {
            e.printStackTrace();
        } catch (TooShortNumberException e) {
            e.printStackTrace();
        }
        return res;
    }
}
