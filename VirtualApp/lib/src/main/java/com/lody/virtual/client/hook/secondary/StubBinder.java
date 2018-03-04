package com.lody.virtual.client.hook.secondary;

import android.content.Context;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import com.lody.virtual.client.core.VirtualCore;

import java.io.FileDescriptor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

/**
 * @author Lody
 */

abstract class StubBinder implements IBinder {
	private ClassLoader mClassLoader;
	private IBinder mBase;
	private IInterface mInterface;
	private Context context;

	StubBinder(Context appContext, ClassLoader classLoader, IBinder base) {
		this.context = appContext;
		this.mClassLoader = classLoader;
		this.mBase = base;
	}

	public String getAppPkg(){
		return context.getPackageName();
	}

	public String getHostPkg(){
		return VirtualCore.get().getHostPkg();
	}

	protected void fakePackage(Object[] args){
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				if (args[i] instanceof String) {
					String str = (String) args[i];
					if (getHostPkg().equals(str)) {
//						args[i] = getAppPkg();
					}
				}
			}
		}
	}

	@Override
	public String getInterfaceDescriptor() throws RemoteException {
		return mBase.getInterfaceDescriptor();
	}

	@Override
	public boolean pingBinder() {
		return mBase.pingBinder();
	}

	@Override
	public boolean isBinderAlive() {
		return mBase.isBinderAlive();
	}


	/**
	 * Anti the Proguard.
	 *
	 * Search the AidlClass.Stub.asInterface(IBinder) method by the StackTrace.
	 *
	 */
	@Override
	public IInterface queryLocalInterface(String descriptor) {
		if (mInterface == null) {
			StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
			if (stackTrace == null || stackTrace.length <= 1) {
				return null;
			}
			Class<?> aidlType = null;
			IInterface targetInterface = null;

			for (StackTraceElement element : stackTrace) {
				if (element.isNativeMethod()) {
					continue;
				}
				try {
                    Method method = mClassLoader.loadClass(element.getClassName())
                            .getDeclaredMethod(element.getMethodName(), IBinder.class);
                    if ((method.getModifiers() & Modifier.STATIC) != 0) {
                        method.setAccessible(true);
                        Class<?> returnType = method.getReturnType();
                        if (returnType.isInterface() && IInterface.class.isAssignableFrom(returnType)) {
                            aidlType = returnType;
                            targetInterface = (IInterface) method.invoke(null, mBase);
                        }
                    }
                } catch (Exception e) {
                    // go to the next cycle
                }
			}
			if (aidlType == null || targetInterface == null) {
                return null;
            }
			InvocationHandler handler = createHandler(aidlType, targetInterface);
			mInterface = (IInterface) Proxy.newProxyInstance(mClassLoader, new Class[]{aidlType}, handler);
		}
		return mInterface;

	}

	public abstract InvocationHandler createHandler(Class<?> interfaceClass, IInterface iInterface);


	@Override
	public void dump(FileDescriptor fd, String[] args) throws RemoteException {
		mBase.dump(fd, args);
	}

	@Override
	public void dumpAsync(FileDescriptor fd, String[] args) throws RemoteException {
		mBase.dumpAsync(fd, args);
	}

	@Override
	public boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
		return mBase.transact(code, data, reply, flags);
	}

	@Override
	public void linkToDeath(DeathRecipient recipient, int flags) throws RemoteException {
		mBase.linkToDeath(recipient, flags);
	}

	@Override
	public boolean unlinkToDeath(DeathRecipient recipient, int flags) {
		return mBase.unlinkToDeath(recipient, flags);
	}
}
