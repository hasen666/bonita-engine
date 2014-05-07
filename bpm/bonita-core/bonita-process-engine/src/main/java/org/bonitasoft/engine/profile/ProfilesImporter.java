/**
 * Copyright (C) 2014 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.profile;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.bonitasoft.engine.api.ImportError;
import org.bonitasoft.engine.api.ImportError.Type;
import org.bonitasoft.engine.api.ImportStatus;
import org.bonitasoft.engine.api.ImportStatus.Status;
import org.bonitasoft.engine.api.impl.SessionInfos;
import org.bonitasoft.engine.bpm.bar.xml.XMLProcessDefinition.BEntry;
import org.bonitasoft.engine.builder.BuilderFactory;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.exception.ExecutionException;
import org.bonitasoft.engine.identity.IdentityService;
import org.bonitasoft.engine.identity.SGroupNotFoundException;
import org.bonitasoft.engine.identity.SRoleNotFoundException;
import org.bonitasoft.engine.identity.SUserNotFoundException;
import org.bonitasoft.engine.identity.model.SGroup;
import org.bonitasoft.engine.identity.model.SRole;
import org.bonitasoft.engine.identity.model.SUser;
import org.bonitasoft.engine.profile.builder.SProfileBuilderFactory;
import org.bonitasoft.engine.profile.builder.SProfileEntryBuilderFactory;
import org.bonitasoft.engine.profile.exception.profile.SProfileCreationException;
import org.bonitasoft.engine.profile.exception.profile.SProfileNotFoundException;
import org.bonitasoft.engine.profile.exception.profile.SProfileUpdateException;
import org.bonitasoft.engine.profile.exception.profileentry.SProfileEntryCreationException;
import org.bonitasoft.engine.profile.exception.profileentry.SProfileEntryDeletionException;
import org.bonitasoft.engine.profile.exception.profilemember.SProfileMemberCreationException;
import org.bonitasoft.engine.profile.exception.profilemember.SProfileMemberDeletionException;
import org.bonitasoft.engine.profile.impl.ExportedParentProfileEntry;
import org.bonitasoft.engine.profile.impl.ExportedProfile;
import org.bonitasoft.engine.profile.impl.ExportedProfileEntry;
import org.bonitasoft.engine.profile.impl.ExportedProfileMapping;
import org.bonitasoft.engine.profile.model.SProfile;
import org.bonitasoft.engine.profile.model.SProfileEntry;
import org.bonitasoft.engine.xml.Parser;
import org.bonitasoft.engine.xml.SValidationException;
import org.bonitasoft.engine.xml.SXMLParseException;

/**
 * 
 * Import profiles with mapping and entries using Policy
 * 
 * 
 * @author Baptiste Mesta
 */
public class ProfilesImporter {

    private final ProfileService profileService;

    private final IdentityService identityService;

    private final List<ExportedProfile> exportedProfiles;

    private final ProfileImportStategy importStrategy;

    /**
     * @param profileService
     * @param identityService
     * @param profiles
     * @param policy
     */
    public ProfilesImporter(final ProfileService profileService, final IdentityService identityService, final List<ExportedProfile> exportedProfiles,
            final ImportPolicy policy) {
        this.profileService = profileService;
        this.identityService = identityService;
        this.exportedProfiles = exportedProfiles;
        switch (policy) {
            case DELETE_EXISTING:
                importStrategy = new DeleteExistingImportStrategy(profileService);
                break;
            case FAIL_ON_DUPLICATES:
                importStrategy = new FailOnDuplicateImportStrategy();
                break;
            case IGNORE_DUPLICATES:
                importStrategy = new IgnoreDuplicateImportStrategy();
                break;
            case REPLACE_DUPLICATES:
                importStrategy = new ReplaceDuplicateImportStrategy(profileService);
                break;
            default:
                importStrategy = null;
                break;

        }
    }

    public List<ImportStatus> importProfiles() throws ExecutionException {

        importStrategy.beforeImport();
        try {
            final List<ImportStatus> importStatus = new ArrayList<ImportStatus>(exportedProfiles.size());
            final long importerId = SessionInfos.getUserIdFromSession();
            for (final ExportedProfile exportedProfile : exportedProfiles) {
                if (exportedProfile.getName() == null || exportedProfile.getName().isEmpty()) {
                    continue;
                }
                ImportStatus currentStatus = new ImportStatus(exportedProfile.getName());
                importStatus.add(currentStatus);
                SProfile existingProfile = null;
                try {
                    existingProfile = profileService.getProfileByName(exportedProfile.getName());
                    currentStatus.setStatus(Status.REPLACED);
                } catch (SProfileNotFoundException e1) {
                    // profile does not exists
                }
                final SProfile newProfile = importTheProfile(importerId, exportedProfile, existingProfile);
                if (newProfile == null) {
                    // in case of skip
                    currentStatus.setStatus(Status.SKIPPED);
                    continue;
                }
                final long profileId = newProfile.getId();

                /*
                 * Import mapping with pages
                 */
                importProfileEntries(profileService, exportedProfile.getParentProfileEntries(), profileId);

                /*
                 * Import mapping with organization
                 */
                currentStatus.getErrors().addAll(importProfileMapping(profileService, identityService, profileId, exportedProfile.getProfileMapping()));
            }
            return importStatus;

        } catch (SBonitaException e) {
            throw new ExecutionException(e);
        }
    }

    private void importProfileEntries(final ProfileService profileService, final List<ExportedParentProfileEntry> parentProfileEntries, final long profileId)
            throws SProfileEntryCreationException {
        for (final ExportedParentProfileEntry parentProfileEntry : parentProfileEntries) {
            /*
             * if( parentProfileEntry.isCustom())
             * find: parentProfileEntry.getPage()
             */
            /*
             * before create check there is at least one child
             */
            final SProfileEntry parentEntry = profileService.createProfileEntry(createProfileEntry(parentProfileEntry, profileId, 0));
            final long parentProfileEntryId = parentEntry.getId();
            final List<ExportedProfileEntry> childrenProEn = parentProfileEntry.getChildProfileEntries();
            if (childrenProEn != null && childrenProEn.size() > 0) {
                for (final ExportedProfileEntry childProfileEntry : childrenProEn) {
                    profileService.createProfileEntry(createProfileEntry(childProfileEntry, profileId, parentProfileEntryId));
                    // TODO check page exists
                }
            }
        }
    }

    private List<ImportError> importProfileMapping(final ProfileService profileService, final IdentityService identityService,
            final long profileId,
            final ExportedProfileMapping exportedProfileMapping) throws SProfileMemberCreationException {
        // TODO if not delete check merge status

        ArrayList<ImportError> errors = new ArrayList<ImportError>();

        for (final String userName : exportedProfileMapping.getUsers()) {
            SUser user = null;
            try {
                user = identityService.getUserByUserName(userName);
            } catch (final SUserNotFoundException e) {
                errors.add(new ImportError(userName, Type.USER));
                continue;
            }
            profileService.addUserToProfile(profileId, user.getId(), user.getFirstName(), user.getLastName(), user.getUserName());
        }
        for (final String roleName : exportedProfileMapping.getRoles()) {
            SRole role = null;
            try {
                role = identityService.getRoleByName(roleName);
            } catch (final SRoleNotFoundException e) {
                errors.add(new ImportError(roleName, Type.ROLE));
                continue;
            }
            profileService.addRoleToProfile(profileId, role.getId(), role.getName());
        }
        for (final String groupPath : exportedProfileMapping.getGroups()) {
            SGroup group = null;
            try {
                group = identityService.getGroupByPath(groupPath);
            } catch (final SGroupNotFoundException e) {
                errors.add(new ImportError(groupPath, Type.GROUP));
                continue;
            }
            profileService.addGroupToProfile(profileId, group.getId(), group.getName(), group.getParentPath());
        }

        for (final BEntry<String, String> membership : exportedProfileMapping.getMemberships()) {
            SGroup group = null;
            try {
                group = identityService.getGroupByPath(membership.getKey());
            } catch (final SGroupNotFoundException e) {
                errors.add(new ImportError(membership.getKey(), Type.GROUP));
            }
            SRole role = null;
            try {
                role = identityService.getRoleByName(membership.getValue());
            } catch (final SRoleNotFoundException e) {
                errors.add(new ImportError(membership.getValue(), Type.ROLE));
            }
            if (group == null || role == null) {
                continue;
            }
            profileService.addRoleAndGroupToProfile(profileId, role.getId(), group.getId(), role.getName(), group.getName(), group.getParentPath());
        }
        return errors;
    }

    private SProfile importTheProfile(final long importerId,
            final ExportedProfile exportedProfile,
            final SProfile existingProfile) throws ExecutionException, SProfileEntryDeletionException, SProfileMemberDeletionException,
            SProfileUpdateException,
            SProfileCreationException {
        final SProfile newProfile;
        if (existingProfile != null) {
            newProfile = importStrategy.whenProfileExists(importerId, exportedProfile, existingProfile);
        } else {
            // create profile
            newProfile = profileService.createProfile(createProfile(exportedProfile, importerId));
        }
        return newProfile;
    }

    private SProfile createProfile(final ExportedProfile exportedProfile, final long importerId) {
        boolean isDefault = exportedProfile.isDefault();
        final long creationDate = System.currentTimeMillis();
        return BuilderFactory.get(SProfileBuilderFactory.class).createNewInstance(exportedProfile.getName(),
                isDefault, creationDate, importerId, creationDate, importerId).setDescription(exportedProfile.getDescription()).done();

    }

    private SProfileEntry createProfileEntry(final ExportedParentProfileEntry exportedProfileEntry, final long profileId, final long parentId) {
        return BuilderFactory.get(SProfileEntryBuilderFactory.class).createNewInstance(exportedProfileEntry.getName(), profileId)
                .setDescription(exportedProfileEntry.getDescription()).setIndex(exportedProfileEntry.getIndex()).setPage(exportedProfileEntry.getPage())
                .setParentId(parentId).setType(exportedProfileEntry.getType()).done();
    }

    private SProfileEntry createProfileEntry(final ExportedProfileEntry exportedProfileEntry, final long profileId, final long parentId) {
        return BuilderFactory.get(SProfileEntryBuilderFactory.class).createNewInstance(exportedProfileEntry.getName(), profileId)
                .setDescription(exportedProfileEntry.getDescription()).setIndex(exportedProfileEntry.getIndex()).setPage(exportedProfileEntry.getPage())
                .setParentId(parentId).setType(exportedProfileEntry.getType()).done();
    }

    public static List<String> toWarnings(final List<ImportStatus> importProfiles) {
        ArrayList<String> warns = new ArrayList<String>();
        for (ImportStatus importStatus : importProfiles) {
            for (ImportError error : importStatus.getErrors()) {
                warns.add("Unable to find the " + error.getType().name().toLowerCase() + " " + error.getName() + " on " + importStatus.getName());
            }
        }
        return warns;
    }

    @SuppressWarnings("unchecked")
    public static List<ExportedProfile> getProfilesFromXML(final String xmlContent, final Parser parser) throws ExecutionException {
        StringReader reader = new StringReader(xmlContent);
        try {
            parser.validate(reader);
            reader.close();
            reader = new StringReader(xmlContent);
            return (List<ExportedProfile>) parser.getObjectFromXML(reader);
        } catch (final IOException ioe) {
            throw new ExecutionException(ioe);
        } catch (final SValidationException e) {
            throw new ExecutionException(e);
        } catch (final SXMLParseException e) {
            throw new ExecutionException(e);
        } finally {
            reader.close();
        }
    }

}
