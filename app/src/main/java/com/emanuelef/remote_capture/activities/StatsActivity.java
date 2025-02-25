/*
 * This file is part of PCAPdroid.
 *
 * PCAPdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PCAPdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PCAPdroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2020-21 - Emanuele Faranda
 */

package com.emanuelef.remote_capture.activities;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TextView;

import com.emanuelef.remote_capture.CaptureService;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.Utils;
import com.emanuelef.remote_capture.model.CaptureStats;

import java.util.Locale;

public class StatsActivity extends BaseActivity {
    private BroadcastReceiver mReceiver;
    private TextView mBytesSent;
    private TextView mBytesRcvd;
    private TextView mPacketsSent;
    private TextView mPacketsRcvd;
    private TextView mActiveConns;
    private TextView mDroppedConns;
    private TextView mDroppedPkts;
    private TextView mTotConns;
    private TextView mHeapUsage;
    private TextView mMemUsage;
    private TextView mLowMem;
    private TextView mOpenSocks;
    private TextView mDnsServer;
    private TextView mDnsQueries;
    private TableLayout mTable;
    private TextView mAllocStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.stats);
        displayBackAction();
        setContentView(R.layout.activity_stats);

        mTable = findViewById(R.id.table);
        mBytesSent = findViewById(R.id.bytes_sent);
        mBytesRcvd = findViewById(R.id.bytes_rcvd);
        mPacketsSent = findViewById(R.id.packets_sent);
        mPacketsRcvd = findViewById(R.id.packets_rcvd);
        mActiveConns = findViewById(R.id.active_connections);
        mDroppedConns = findViewById(R.id.dropped_connections);
        mDroppedPkts = findViewById(R.id.pkts_dropped);
        mTotConns = findViewById(R.id.tot_connections);
        mHeapUsage = findViewById(R.id.heap_usage);
        mMemUsage = findViewById(R.id.mem_usage);
        mLowMem = findViewById(R.id.low_mem_detected);
        mOpenSocks = findViewById(R.id.open_sockets);
        mDnsQueries = findViewById(R.id.dns_queries);
        mDnsServer = findViewById(R.id.dns_server);
        mAllocStats = findViewById(R.id.alloc_stats);

        if(CaptureService.isCapturingAsRoot()) {
            findViewById(R.id.open_sockets_row).setVisibility(View.GONE);
            findViewById(R.id.row_dropped_connections).setVisibility(View.GONE);
        } else
            findViewById(R.id.row_pkts_dropped).setVisibility(View.GONE);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateStats(intent);
            }
        };

        /* Register for updates */
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mReceiver, new IntentFilter(CaptureService.ACTION_STATS_DUMP));

        CaptureService.askStatsDump();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(mReceiver != null)
            LocalBroadcastManager.getInstance(this)
                    .unregisterReceiver(mReceiver);
    }

    private void updateStats(Intent intent) {
        CaptureStats stats = (CaptureStats) intent.getSerializableExtra("value");

        mBytesSent.setText(Utils.formatBytes(stats.bytes_sent));
        mBytesRcvd.setText(Utils.formatBytes(stats.bytes_rcvd));
        mPacketsSent.setText(Utils.formatIntShort(stats.pkts_sent));
        mPacketsRcvd.setText(Utils.formatIntShort(stats.pkts_rcvd));
        mActiveConns.setText(Utils.formatNumber(this, stats.active_conns));
        mDroppedConns.setText(Utils.formatNumber(this, stats.num_dropped_conns));
        mDroppedPkts.setText(Utils.formatNumber(this, stats.pkts_dropped));
        mTotConns.setText(Utils.formatNumber(this, stats.tot_conns));
        mOpenSocks.setText(Utils.formatNumber(this, stats.num_open_sockets));
        mDnsQueries.setText(Utils.formatNumber(this, stats.num_dns_queries));

        updateMemoryStats();

        if(!CaptureService.isDNSEncrypted()) {
            findViewById(R.id.dns_server_row).setVisibility(View.VISIBLE);
            findViewById(R.id.dns_queries_row).setVisibility(View.VISIBLE);
            mDnsServer.setText(CaptureService.getDNSServer());
        } else {
            findViewById(R.id.dns_server_row).setVisibility(View.GONE);
            findViewById(R.id.dns_queries_row).setVisibility(View.GONE);
        }

        if(stats.num_dropped_conns > 0)
            mDroppedConns.setTextColor(Color.RED);

        if(stats.alloc_summary != null) {
            mAllocStats.setVisibility(View.VISIBLE);
            mAllocStats.setText(stats.alloc_summary);
        }
    }

    private void updateMemoryStats() {
        Locale locale = Utils.getPrimaryLocale(this);
        long heapAvailable = Utils.getAvailableHeap();
        long heapMax = Runtime.getRuntime().maxMemory();

        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        mHeapUsage.setText(String.format(locale, "%s / %s (%d%%)", Utils.formatBytes(heapMax - heapAvailable),
                Utils.formatBytes(heapMax),
                (heapMax - heapAvailable) * 100 / heapMax));

        mMemUsage.setText(String.format(locale, "%s / %s (%d%%)", Utils.formatBytes(memoryInfo.totalMem - memoryInfo.availMem),
                Utils.formatBytes(memoryInfo.totalMem),
                (memoryInfo.totalMem - memoryInfo.availMem) * 100 / memoryInfo.totalMem));

        mLowMem.setText(getString(CaptureService.isLowMemory() ? R.string.yes : R.string.no));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.copy_share_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    private String getContents() {
        return Utils.table2Text(mTable);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.copy_to_clipboard) {
            Utils.copyToClipboard(this, getContents());
            return true;
        } else if(id == R.id.share) {
            Utils.shareText(this, getString(R.string.stats), getContents());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}