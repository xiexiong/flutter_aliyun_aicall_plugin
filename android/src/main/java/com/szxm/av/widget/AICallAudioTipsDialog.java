package com.szxm.av.widget;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.OnDismissListener;
import com.orhanobut.dialogplus.ViewHolder;
import com.szxm.av.R;
import com.szxm.av.controller.ARTCAICallController;
import com.szxm.av.utils.DisplayUtil;

public class AICallAudioTipsDialog {

    public static void show(Context context, ARTCAICallController artcaiCallController) {
        ViewGroup view = new FrameLayout(context);
        view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                DisplayUtil.dip2px(420)));
//        view.setBackgroundColor(0x80FFFFFF);
        view.setBackgroundResource(R.color.layout_base_black_alpha_50);

        ViewHolder viewHolder = new ViewHolder(view);
        DialogPlus dialog = DialogPlus.newDialog(context)
                .setContentHolder(viewHolder)
                .setGravity(Gravity.CENTER)
                .setExpanded(true, DisplayUtil.dip2px(420))
                .setOverlayBackgroundResource(android.R.color.transparent)
                .setContentBackgroundResource(R.color.layout_base_black_alpha_50)
                .setOnClickListener((dialog1, v) -> {

                })
                .setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogPlus dialog) {
                        artcaiCallController.showARTCDebugView(null, 0, "");
                    }
                })
                .create();
        dialog.show();

        artcaiCallController.showARTCDebugView(view, 4, "");
    }
}
