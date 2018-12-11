/*
 * Copyright (c) 2018 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.provider.linux.syscall;

import android.system.ErrnoException;
import android.system.OsConstants;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java8.nio.file.AccessDeniedException;
import java8.nio.file.DirectoryNotEmptyException;
import java8.nio.file.FileAlreadyExistsException;
import java8.nio.file.FileSystemException;
import java8.nio.file.FileSystemLoopException;
import java8.nio.file.NoSuchFileException;
import java8.nio.file.NotDirectoryException;
import java8.nio.file.ReadOnlyFileSystemException;
import me.zhanghai.android.files.reflected.ReflectedAccessor;
import me.zhanghai.android.files.reflected.ReflectedField;
import me.zhanghai.android.files.reflected.RestrictedHiddenApi;

public class SyscallException extends Exception {

    @NonNull
    private final String mFunctionName;

    private final int mErrno;

    public SyscallException(@NonNull String functionName, int errno,
                            @Nullable Throwable cause) {
        super(perror(errno, functionName), cause);

        mFunctionName = functionName;
        mErrno = errno;
    }

    public SyscallException(@NonNull String functionName, int errno) {
        this(functionName, errno, null);
    }

    public SyscallException(@NonNull ErrnoException errnoException) {
        this(ErrnoExceptionCompat.getFunctionName(errnoException), errnoException.errno,
                errnoException);
    }

    @NonNull
    public String getFunctionName() {
        return mFunctionName;
    }

    public int getErrno() {
        return mErrno;
    }

    @NonNull
    private static String perror(int errno, @NonNull String functionName) {
        return functionName + ": " + Syscalls.strerror(errno);
    }

    public void rethrowAsFileSystemException(@Nullable String file, @Nullable String other)
            throws FileSystemException {
        FileSystemException fileSystemException = toFileSystemException(file, other);
        fileSystemException.initCause(this);
        throw fileSystemException;
    }

    private FileSystemException toFileSystemException(@Nullable String file,
                                                      @Nullable String other) {
        if (mErrno == OsConstants.EACCES) {
            return new AccessDeniedException(file, other, getMessage());
        } else if (mErrno == OsConstants.EEXIST) {
            return new FileAlreadyExistsException(file, other, getMessage());
        } else if (mErrno == OsConstants.ELOOP) {
            return new FileSystemLoopException(file);
        } else if (mErrno == OsConstants.ENOTDIR) {
            return new NotDirectoryException(file);
        } else if (mErrno == OsConstants.ENOTEMPTY) {
            return new DirectoryNotEmptyException(file);
        } else if (mErrno == OsConstants.ENOENT) {
            return new NoSuchFileException(file, other, getMessage());
        } else {
            return new FileSystemException(file, other, getMessage());
        }
    }

    private static class ErrnoExceptionCompat {

        static {
            ReflectedAccessor.allowRestrictedHiddenApiAccess();
        }

        @RestrictedHiddenApi
        private static final ReflectedField sFunctionNameField = new ReflectedField(
                ErrnoException.class, "functionName");

        private ErrnoExceptionCompat() {}

        @NonNull
        public static String getFunctionName(@NonNull ErrnoException errnoException) {
            return sFunctionNameField.getObject(errnoException);
        }
    }
}
