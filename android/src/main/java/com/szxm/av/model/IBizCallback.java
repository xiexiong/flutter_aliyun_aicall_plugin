package com.szxm.av.model;

import java.util.List;

public interface IBizCallback<T> {
    void onSuccess(List<T> data);
    void onError(int code, String msg);
}
