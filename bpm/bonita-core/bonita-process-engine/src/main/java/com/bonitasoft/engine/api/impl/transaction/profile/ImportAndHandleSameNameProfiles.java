/*******************************************************************************
 * Copyright (C) 2012-2013 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 *******************************************************************************/
package com.bonitasoft.engine.api.impl.transaction.profile;

import java.util.ArrayList;
import java.util.List;

import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.commons.transaction.TransactionContentWithResult;
import org.bonitasoft.engine.identity.IdentityService;
import org.bonitasoft.engine.profile.ProfileService;
import org.bonitasoft.engine.profile.SProfileAlreadyExistsException;
import org.bonitasoft.engine.profile.SProfileCreationException;
import org.bonitasoft.engine.profile.SProfileEntryAlreadyExistsException;
import org.bonitasoft.engine.profile.SProfileEntryCreationException;
import org.bonitasoft.engine.profile.SProfileNotFoundException;
import org.bonitasoft.engine.profile.builder.SProfileBuilder;
import org.bonitasoft.engine.profile.builder.SProfileBuilderAccessor;
import org.bonitasoft.engine.profile.builder.SProfileEntryBuilder;
import org.bonitasoft.engine.profile.impl.ExportedParentProfileEntry;
import org.bonitasoft.engine.profile.impl.ExportedProfile;
import org.bonitasoft.engine.profile.impl.ExportedProfileEntry;
import org.bonitasoft.engine.profile.impl.ExportedProfileMapping;
import org.bonitasoft.engine.profile.model.SProfile;
import org.bonitasoft.engine.profile.model.SProfileEntry;

import com.bonitasoft.engine.profile.ImportPolicy;
import com.bonitasoft.engine.profile.xml.SProfileImportDuplicatedException;

/**
 * @author Zhao Na
 * @author Celine Souchet
 */
public class ImportAndHandleSameNameProfiles implements TransactionContentWithResult<List<String>> {

    private final ProfileService profileService;

    private final IdentityService identityService;

    private final List<ExportedProfile> profiles;

    private final List<String> warnings = new ArrayList<String>();

    private final ImportPolicy policy;

    public ImportAndHandleSameNameProfiles(final ProfileService profileService, final IdentityService identityService, final List<ExportedProfile> profiles,
            final ImportPolicy policy) {
        super();
        this.profileService = profileService;
        this.identityService = identityService;
        this.profiles = profiles;
        this.policy = policy;
    }

    @Override
    public void execute() throws SBonitaException {
        for (final ExportedProfile profile : profiles) {
            if (profile.getName() != null && !"".equals(profile.getName())) {
                try {
                    profileService.getProfileByName(profile.getName());

                    // final SProfile existingProfile = profileService.getProfileByName(profile.getName());
                    // if (ImportPolicy.REPLACE_DUPLICATES.equals(policy)) {
                    // // delete duplicated ones
                    // final DeleteProfile deleteProfile = new DeleteProfile(profileService, existingProfile.getId());
                    // deleteProfile.execute();
                    // } else
                    if (ImportPolicy.IGNORE_DUPLICATES.equals(policy)) {
                        continue;
                    } else if (ImportPolicy.FAIL_ON_DUPLICATES.equals(policy)) {
                        throw new SProfileImportDuplicatedException("There's a same name profile when import a profile named " + profile.getName());
                    }
                } catch (final SProfileNotFoundException e) {
                }

                // insert profile
                final long profileId = insertProfile(profile);

                // insert profileEntries
                final List<ExportedParentProfileEntry> parentProfileEntries = profile.getParentProfileEntries();
                for (final ExportedParentProfileEntry parentProfileEntry : parentProfileEntries) {
                    insertParentProfileEntry(profileId, parentProfileEntry);
                }
                // insert profileMapping
                final List<String> warns = insertProfileMapping(profile.getProfileMapping(), profileId);
                if (warns != null && !warns.isEmpty()) {
                    warnings.addAll(warns);
                }
            }
        }
    }

    private long insertProfile(final ExportedProfile profile) throws SProfileAlreadyExistsException, SProfileCreationException {
        final SProfileBuilderAccessor builders = profileService.getSProfileBuilderAccessor();
        final SProfileBuilder profileBuilder = builders.getSProfileBuilder();

        final SProfile sProfile = profileBuilder.createNewInstance(profile.getName()).setDescription(profile.getDescription())
                .setIconPath(profile.getIconPath()).done();
        return profileService.createProfile(sProfile).getId();
    }

    private void insertParentProfileEntry(final long profileId, final ExportedParentProfileEntry parentprofileEntry)
            throws SProfileEntryAlreadyExistsException, SProfileEntryCreationException {
        final SProfileBuilderAccessor builders = profileService.getSProfileBuilderAccessor();
        final SProfileEntryBuilder proEntryBuilder = builders.getSProfileEntryBuilder();

        final SProfileEntry sproEntry = proEntryBuilder.createNewInstance(parentprofileEntry.getName(), profileId)
                .setDescription(parentprofileEntry.getDescription()).setIndex(parentprofileEntry.getIndex()).setPage(parentprofileEntry.getPage())
                .setParentId(0).setType(parentprofileEntry.getType()).done();
        final SProfileEntry parentEntry = profileService.createProfileEntry(sproEntry);

        final List<ExportedProfileEntry> childrenProEn = parentprofileEntry.getChildProfileEntries();
        if (childrenProEn != null) {
            for (final ExportedProfileEntry childProfileEntry : childrenProEn) {
                insertChildProfileEntry(profileId, parentEntry.getId(), childProfileEntry);
            }
        }
    }

    private void insertChildProfileEntry(final long profileId, final long parentId, final ExportedProfileEntry childProfileEntry)
            throws SProfileEntryAlreadyExistsException, SProfileEntryCreationException {
        final SProfileBuilderAccessor builders = profileService.getSProfileBuilderAccessor();
        final SProfileEntryBuilder proEntryBuilder = builders.getSProfileEntryBuilder();

        final SProfileEntry sproEntrytp = proEntryBuilder.createNewInstance(childProfileEntry.getName(), profileId)
                .setDescription(childProfileEntry.getDescription()).setIndex(childProfileEntry.getIndex()).setPage(childProfileEntry.getPage())
                .setParentId(parentId).setType(childProfileEntry.getType()).done();
        profileService.createProfileEntry(sproEntrytp);
    }

    @Override
    public List<String> getResult() {
        return warnings;
    }

    private List<String> insertProfileMapping(final ExportedProfileMapping profileMapp, final long profileId) throws SBonitaException {
        final ImportProfileMember importpm = new ImportProfileMember(profileService, identityService, profileMapp, profileId);
        importpm.execute();
        final List<String> warns = importpm.getResult();
        return warns;
    }

}
