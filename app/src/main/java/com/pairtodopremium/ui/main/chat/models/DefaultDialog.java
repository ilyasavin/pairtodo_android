package com.pairtodopremium.ui.main.chat.models;

import com.stfalcon.chatkit.commons.models.IDialog;
import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.commons.models.IUser;
import java.util.ArrayList;

/**
 * Created by Anton Bevza on 12/9/16.
 */

public class DefaultDialog implements IDialog {
  private String id;

  private String dialogPhoto;

  private String dialogName;

  private ArrayList<IUser> users;

  private IMessage lastMessage;

  private int unreadCount;

  public DefaultDialog(String id, String name, String photo, ArrayList<IUser> users,
      IMessage lastMessage, int unreadCount) {
    this.id = id;
    this.dialogName = name;
    this.dialogPhoto = photo;
    this.users = users;
    this.lastMessage = lastMessage;
    this.unreadCount = unreadCount;
  }

  @Override public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override public String getDialogPhoto() {
    return dialogPhoto;
  }

  @Override public String getDialogName() {
    return dialogName;
  }

  @Override public ArrayList<IUser> getUsers() {
    return users;
  }

  public void setUsers(ArrayList<IUser> users) {
    this.users = users;
  }

  @Override public IMessage getLastMessage() {
    return lastMessage;
  }

  @Override public void setLastMessage(IMessage lastMessage) {
    this.lastMessage = lastMessage;
  }

  @Override public int getUnreadCount() {
    return unreadCount;
  }

  public void setUnreadCount(int unreadCount) {
    this.unreadCount = unreadCount;
  }
}
