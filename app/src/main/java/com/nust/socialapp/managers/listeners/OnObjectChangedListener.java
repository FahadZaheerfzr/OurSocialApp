

package com.nust.socialapp.managers.listeners;

public interface OnObjectChangedListener<T> {

    void onObjectChanged(T obj);

    void onError(String errorText);
}
