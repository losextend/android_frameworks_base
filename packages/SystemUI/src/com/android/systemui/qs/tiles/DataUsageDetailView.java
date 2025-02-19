/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.telephony.SubscriptionInfo;
import android.text.BidiFormatter;
import android.text.format.Formatter;
import android.text.format.Formatter.BytesResult;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.android.settingslib.Utils;
import com.android.settingslib.net.DataUsageController;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.qs.DataUsageGraph;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Layout for the data usage detail in quick settings.
 */
public class DataUsageDetailView extends LinearLayout {

    private final DecimalFormat FORMAT = new DecimalFormat("#.##");
    private DataSimSwitchListener mDataSimSwitchListener;
    private RadioGroup.OnCheckedChangeListener mSubsGroupListener = new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (mDataSimSwitchListener == null) return;
                mDataSimSwitchListener.onSwitch(checkedId);
            }
        };

    public DataUsageDetailView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FontSizeUtils.updateFontSize(this, android.R.id.title, R.dimen.qs_data_usage_text_size);
        FontSizeUtils.updateFontSize(this, R.id.usage_text, R.dimen.qs_data_usage_usage_text_size);
        FontSizeUtils.updateFontSize(this, R.id.usage_carrier_text,
                R.dimen.qs_data_usage_text_size);
        FontSizeUtils.updateFontSize(this, R.id.usage_info_top_text,
                R.dimen.qs_data_usage_text_size);
        FontSizeUtils.updateFontSize(this, R.id.usage_period_text, R.dimen.qs_data_usage_text_size);
        FontSizeUtils.updateFontSize(this, R.id.usage_info_bottom_text,
                R.dimen.qs_data_usage_text_size);
        FontSizeUtils.updateFontSize(this, R.id.data_sim_title, R.dimen.qs_data_usage_text_size);
    }

    public void bind(DataUsageController.DataUsageInfo info) {
        final Resources res = mContext.getResources();
        final int titleId;
        final long bytes;
        ColorStateList usageColorState = null;
        final String top;
        String bottom = null;
        if (info.usageLevel < info.warningLevel || info.limitLevel <= 0) {
            // under warning, or no limit
            titleId = R.string.quick_settings_cellular_detail_data_usage;
            bytes = info.usageLevel;
            top = res.getString(R.string.quick_settings_cellular_detail_data_warning,
                    formatDataUsage(info.warningLevel));
        } else if (info.usageLevel <= info.limitLevel) {
            // over warning, under limit
            titleId = R.string.quick_settings_cellular_detail_remaining_data;
            bytes = info.limitLevel - info.usageLevel;
            top = res.getString(R.string.quick_settings_cellular_detail_data_used,
                    formatDataUsage(info.usageLevel));
            bottom = res.getString(R.string.quick_settings_cellular_detail_data_limit,
                    formatDataUsage(info.limitLevel));
        } else {
            // over limit
            titleId = R.string.quick_settings_cellular_detail_over_limit;
            bytes = info.usageLevel - info.limitLevel;
            top = res.getString(R.string.quick_settings_cellular_detail_data_used,
                    formatDataUsage(info.usageLevel));
            bottom = res.getString(R.string.quick_settings_cellular_detail_data_limit,
                    formatDataUsage(info.limitLevel));
            usageColorState = Utils.getColorError(mContext);
        }

        if (usageColorState == null) {
            usageColorState = Utils.getColorAccent(mContext);
        }

        final TextView title = findViewById(android.R.id.title);
        title.setText(titleId);
        final TextView usage = findViewById(R.id.usage_text);
        usage.setText(formatDataUsage(bytes));
        usage.setTextColor(usageColorState);
        final DataUsageGraph graph = findViewById(R.id.usage_graph);
        graph.setLevels(info.limitLevel, info.warningLevel, info.usageLevel);
        final TextView carrier = findViewById(R.id.usage_carrier_text);
        carrier.setText(info.carrier);
        final TextView period = findViewById(R.id.usage_period_text);
        period.setText(info.period);
        final TextView infoTop = findViewById(R.id.usage_info_top_text);
        infoTop.setVisibility(top != null ? View.VISIBLE : View.GONE);
        infoTop.setText(top);
        final TextView infoBottom = findViewById(R.id.usage_info_bottom_text);
        infoBottom.setVisibility(bottom != null ? View.VISIBLE : View.GONE);
        infoBottom.setText(bottom);
        boolean showLevel = info.warningLevel > 0 || info.limitLevel > 0;
        graph.setVisibility(showLevel ? View.VISIBLE : View.GONE);
        if (!showLevel) {
            infoTop.setVisibility(View.GONE);
        }

    }

    private CharSequence formatDataUsage(long byteValue) {
        final BytesResult res = Formatter.formatBytes(mContext.getResources(), byteValue,
                Formatter.FLAG_IEC_UNITS);
        return BidiFormatter.getInstance().unicodeWrap(mContext.getString(
                com.android.internal.R.string.fileSizeSuffix, res.value, res.units));
    }

    public void setSubInfoList(List<SubscriptionInfo> subInfoList, int selectedSubId) {
        final LinearLayout subsLayout = findViewById(R.id.data_sim_switch);
        final RadioGroup subsGroup = findViewById(R.id.data_sim_group);

        if (subInfoList != null && subInfoList.size() > 1) {
            subsGroup.removeAllViews();
            for (SubscriptionInfo info : subInfoList) {
                RadioButton rb = new RadioButton(mContext);
                rb.setId(info.getSubscriptionId());
                rb.setText(info.getCarrierName());
                rb.setChecked(selectedSubId == info.getSubscriptionId());
                subsGroup.addView(rb);
            }
            subsLayout.setVisibility(View.VISIBLE);
        } else {
            subsLayout.setVisibility(View.GONE);
        }
    }

    public void setDataSimSwitchListener(DataSimSwitchListener listener) {
        RadioGroup subsGroup = findViewById(R.id.data_sim_group);
        subsGroup.setOnCheckedChangeListener(mSubsGroupListener);
        mDataSimSwitchListener = listener;
    }

    public interface DataSimSwitchListener {
        void onSwitch(int subscriptionId);
    }
}
