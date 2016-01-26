package org.ccci.idm.user;

import com.google.common.annotations.Beta;

import javax.annotation.Nullable;
import java.io.Serializable;

@Beta
public final class SearchQuery implements Serializable {
    private static final long serialVersionUID = 1188289784574984992L;

    @Nullable
    private String email;
    @Nullable
    private String firstName;
    @Nullable
    private String lastName;
    @Nullable
    private String employeeId;
    @Nullable
    private Group group;

    private boolean includeDeactivated = false;

    public SearchQuery email(@Nullable final String pattern) {
        this.email = pattern;
        return this;
    }

    public SearchQuery firstName(@Nullable final String pattern) {
        this.firstName = pattern;
        return this;
    }

    public SearchQuery lastName(@Nullable final String pattern) {
        this.lastName = pattern;
        return this;
    }

    public SearchQuery employeeId(@Nullable final String id) {
        this.employeeId = id;
        return this;
    }

    public SearchQuery group(@Nullable final Group group) {
        this.group = group;
        return this;
    }

    public SearchQuery includeDeactivated(final boolean include) {
        this.includeDeactivated = include;
        return this;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    @Nullable
    public String getFirstName() {
        return firstName;
    }

    @Nullable
    public String getLastName() {
        return lastName;
    }

    @Nullable
    public String getEmployeeId() {
        return employeeId;
    }

    @Nullable
    public Group getGroup() {
        return group;
    }

    public boolean isIncludeDeactivated() {
        return includeDeactivated;
    }
}
