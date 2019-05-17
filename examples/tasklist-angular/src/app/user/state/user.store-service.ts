import {Injectable} from '@angular/core';
import {Dispatch, Select, StoreService} from '@ngxp/store-service';
import {UserProfile, UserState} from './user.reducer';
import {availableUserIds, currentUserProfile} from './user.selectors';
import {Observable} from 'rxjs';
import {LoadAvailableUsersAction, SelectUserAction} from './user.actions';

@Injectable()
export class UserStoreService extends StoreService<UserState> {

  @Select(availableUserIds)
  availableUserIds$: () => Observable<string[]>;

  @Select(currentUserProfile)
  currentUserProfile$: () => Observable<UserProfile>;

  @Dispatch(LoadAvailableUsersAction)
  loadAvailableUsers: () => void;

  @Dispatch(SelectUserAction)
  selectUser: (string) => void;
}
