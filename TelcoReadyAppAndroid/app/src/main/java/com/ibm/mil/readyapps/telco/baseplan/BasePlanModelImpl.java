/*
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 */

package com.ibm.mil.readyapps.telco.baseplan;



import android.util.Log;
import com.ibm.mil.readyapps.telco.analytics.AnalyticsCnsts;
import com.ibm.mil.readyapps.telco.analytics.OperationalAnalyticsReporter;

import com.worklight.wlclient.api.WLFailResponse;
import com.worklight.wlclient.api.WLResourceRequest;
import com.worklight.wlclient.api.WLResponse;
import com.worklight.wlclient.api.WLResponseListener;


import java.net.URI;

import rx.Observable;
import rx.subjects.PublishSubject;

import static com.ibm.mil.readyapps.telco.utils.LoginActivity.credentials;


/**
 * Implementation of BasePlanModel for updating BasePlan POJO
 * and emitting streams upon updating so subscribers are notified
 * of changes.
 */
public class BasePlanModelImpl implements BasePlanModel {

    private static final BasePlan basePlan = new BasePlan();
    private static final PublishSubject<BasePlan> basePlanStream = PublishSubject.create();


    /**
     * Get the stream for subscribing to updates.
     *
     * @return the stream for base plan
     */
    @Override
    public Observable<BasePlan> getBasePlanStream() {
        return Observable.merge(Observable.just(basePlan), basePlanStream);
    }

    /**
     * Update the base cost of the user's plan and
     * emit stream to all subscribers.
     *
     * @param changeAmount the cost to update by
     */
    @Override
    public void updateBaseCost(double changeAmount) {

        if (changeAmount >= 0) {
            OperationalAnalyticsReporter.planChangeAccepted(AnalyticsCnsts.BASE_INCREASE, changeAmount);
        } else {
            OperationalAnalyticsReporter.planChangeAccepted(AnalyticsCnsts.BASE_DECREASE, changeAmount);
        }

        basePlan.setBaseCost(basePlan.getBaseCost() + changeAmount);
        updateBasePlanMFP();
        basePlanStream.onNext(basePlan);
    }

    /**
     * Update the additional cost for this cycle of user's plan
     * and emit stream to all subscribers.
     *
     * @param changeAmount cost to update by
     */
    @Override
    public void updateAddonCost(double changeAmount) {

        OperationalAnalyticsReporter.planChangeAccepted(AnalyticsCnsts.ADDON, changeAmount);

        basePlan.setAddonCost(basePlan.getAddonCost() + changeAmount);
        updateBasePlanMFP();
        basePlanStream.onNext(basePlan);
    }

    /**
     * Helper method for deciding which section of cost to update
     * (base or addon) based on arguments passed in.
     *
     * @param changeAmount the cost to update by
     * @param affectsBaseCost whether or not this cost update should affect base
     */
    @Override
    public void updateCost(double changeAmount, boolean affectsBaseCost) {
        if (affectsBaseCost) {
            updateBaseCost(changeAmount);
        } else {
            updateAddonCost(changeAmount);
        }
    }

    private void updateBasePlanMFP() {

        String userid=null;
        try {
            userid = credentials.getString("username");
        }
        catch (Exception e){

        }


        try {

            URI uri = new URI("adapters/TelcoUserAdapter/users/" + userid);
            WLResourceRequest request = new WLResourceRequest(uri, WLResourceRequest.GET);
            request.send(new WLResponseListener(){

                public void onSuccess(WLResponse response) {
                    Log.i("BasePlanModelImpl", BasePlanModelImpl.class.getName() + " " + response
                            .getResponseText());

                }
                public void onFailure(WLFailResponse response) {
                    Log.i("BasePlanModelImpl", "UPDATE FAILED");
                }

            });

        }
        catch(Exception e) {

        }
    }

}
