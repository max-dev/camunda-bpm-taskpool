import {UserActions, UserActionTypes} from './user.actions';

export interface UserProfile {
  userIdentifier: string;
  username: string;
  fullName: string;
}

export interface UserState {
  currentUserId: string;
  availableUsers: {[key: string]: string};
  currentUserProfile: UserProfile;
}

const initialState: UserState = {
  currentUserId: null,
  availableUsers: {},
  currentUserProfile: {
    userIdentifier: '',
    username: '',
    fullName: ''
  }
};

export function userReducer(state: UserState = initialState, action: UserActions): UserState {
  switch (action.type) {

    case UserActionTypes.AvailableUsersLoaded:
      return {
        ...state,
        availableUsers: action.payload
      };

    case UserActionTypes.SelectUser:
      return {
        ...state,
        currentUserId: action.payload
      };

    case UserActionTypes.UserProfileLoaded:
      return {
        ...state,
        currentUserProfile: action.payload
      };

    default:
      return state;
  }
}
