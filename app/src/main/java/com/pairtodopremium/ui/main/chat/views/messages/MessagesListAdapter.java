package com.pairtodopremium.ui.main.chat.views.messages;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.pairtodopremium.R;
import com.pairtodopremium.utils.Constants;
import com.squareup.picasso.Picasso;
import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.commons.ViewHolder;
import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.utils.DateFormatter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Adapter for {@link MessagesList}.
 */
public class MessagesListAdapter<MESSAGE extends IMessage> extends RecyclerView.Adapter<ViewHolder>
    implements RecyclerScrollMoreListener.OnLoadMoreListener {

  private static final int VIEW_TYPE_INCOMING_MESSAGE = 0x00;
  private static final int VIEW_TYPE_OUTCOMING_MESSAGE = 0x01;
  private static final int VIEW_TYPE_DATE_HEADER = 0x02;
  private static Context context;
  private HoldersConfig holders;
  private String senderId;
  private List<Wrapper> items;
  private int selectedItemsCount;
  private boolean isSelectMode;
  private SelectionListener selectionListener;

  private OnLoadMoreListener loadMoreListener;
  private OnMessageClickListener<MESSAGE> onMessageClickListener;
  private OnMessageLongClickListener<MESSAGE> onMessageLongClickListener;
  private ImageLoader imageLoader;
  private RecyclerView.LayoutManager layoutManager;
  private MessagesListStyle messagesListStyle;

  /**
   * For default list item layout and view holder.
   *
   * @param senderId identifier of sender.
   * @param holders custom layouts and view holders. See {@link HoldersConfig} documentation for
   * details
   * @param imageLoader image loading method.
   */
  public MessagesListAdapter(Context context, String senderId, HoldersConfig holders,
      ImageLoader imageLoader) {
    this.context = context;
    this.senderId = senderId;
    this.holders = holders;
    this.imageLoader = imageLoader;
    this.items = new ArrayList<>();
  }

  public static boolean checkIfStickerEncode(String message) {
    return message.contains(Constants.STICKER);
  }

  public static String getStickerPath(String message) {
    StringBuilder stringBuilder = new StringBuilder();
    String path = "file:///android_asset/stickers/";
    stringBuilder.append(path);
    stringBuilder.append(message.substring(Constants.STICKER.length(), message.indexOf("_")));
    stringBuilder.append("/");
    stringBuilder.append("b");
    String stickerNumber = message.substring(message.indexOf("_") + 1, message.length());
    stringBuilder.append(stickerNumber);
    stringBuilder.append("@2x.png");
    Log.e("STIIIIIIIIIICKER", stringBuilder.toString());
    return stringBuilder.toString();
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    switch (viewType) {
      case VIEW_TYPE_INCOMING_MESSAGE:
        return getHolder(parent, holders.incomingLayout, holders.incomingHolder);
      case VIEW_TYPE_OUTCOMING_MESSAGE:
        return getHolder(parent, holders.outcomingLayout, holders.outcomingHolder);
      case VIEW_TYPE_DATE_HEADER:
        return getHolder(parent, holders.dateHeaderLayout, holders.dateHeaderHolder);
    }
    return null;
  }

  private <HOLDER extends ViewHolder> ViewHolder getHolder(ViewGroup parent, @LayoutRes int layout,
      Class<HOLDER> holderClass) {
    View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);

    try {
      Constructor<HOLDER> constructor = holderClass.getDeclaredConstructor(View.class);
      constructor.setAccessible(true);
      HOLDER holder = constructor.newInstance(v);
      if (holder instanceof DefaultMessageViewHolder) {
        ((DefaultMessageViewHolder) holder).applyStyle(messagesListStyle);
      }
      return holder;
    } catch (Exception e) {
      return null;
    }
  }

  @SuppressWarnings("unchecked") @Override
  public void onBindViewHolder(ViewHolder holder, int position) {
    Wrapper wrapper = items.get(position);

    if (wrapper.item instanceof IMessage) {
      ((BaseMessageViewHolder) holder).isSelected = wrapper.isSelected;
      ((BaseMessageViewHolder) holder).imageLoader = this.imageLoader;
      holder.itemView.setOnLongClickListener(getMessageLongClickListener(wrapper));
      holder.itemView.setOnClickListener(getMessageClickListener(wrapper));
      if (((IMessage) wrapper.item).getText().contains(Constants.STICKER)) {
        int a = 5;
      }
    }

    holder.onBind(wrapper.item);
  }

  @Override public int getItemCount() {
    return items.size();
  }

    /*
    * PUBLIC METHODS
    * */

  @Override public int getItemViewType(int position) {
    Wrapper wrapper = items.get(position);
    if (wrapper.item instanceof IMessage) {
      IMessage message = (IMessage) wrapper.item;
      if (message.getUser().getId().contentEquals(senderId)) {
        return VIEW_TYPE_OUTCOMING_MESSAGE;
      } else {
        return VIEW_TYPE_INCOMING_MESSAGE;
      }
    } else {
      return VIEW_TYPE_DATE_HEADER;
    }
  }

  @Override public void onLoadMore(int page, int total) {
    if (loadMoreListener != null) {
      loadMoreListener.onLoadMore(page, total);
    }
  }

  /**
   * Adds message to bottom of list and scroll if needed.
   *
   * @param message message to add.
   * @param scroll {@code true} if need to scroll list to bottom when message added.
   */
  public void addToStart(MESSAGE message, boolean scroll) {
    boolean isNewMessageToday = !isPreviousSameDate(0, message.getCreatedAt());
    if (isNewMessageToday) {
      items.add(0, new Wrapper<>(message.getCreatedAt()));
    }
    Wrapper<MESSAGE> element = new Wrapper<>(message);
    items.add(0, element);
    notifyItemRangeInserted(0, isNewMessageToday ? 2 : 1);
    if (layoutManager != null && scroll) {
      layoutManager.scrollToPosition(0);
    }
  }

  /**
   * Adds messages list in chronological order. Use this method to add history.
   *
   * @param messages messages from history.
   * @param reverse {@code true} if need to reverse messages before adding.
   */
  public void addToEnd(List<MESSAGE> messages, boolean reverse) {
    if (reverse) Collections.reverse(messages);

    if (!items.isEmpty()) {
      int lastItemPosition = items.size() - 1;
      Date lastItem = (Date) items.get(lastItemPosition).item;
      if (DateFormatter.isSameDay(messages.get(0).getCreatedAt(), lastItem)) {
        items.remove(lastItemPosition);
        notifyItemRemoved(lastItemPosition);
      }
    }

    int oldSize = items.size();
    generateDateHeaders(messages);
    notifyItemRangeInserted(oldSize, items.size() - oldSize);
  }

  /**
   * Updates message by its id.
   *
   * @param message updated message object.
   */
  public void update(MESSAGE message) {
    update(message.getId(), message);
  }

  /**
   * Updates message by old identifier (use this method if id has changed). Otherwise use {@link
   * #update(IMessage)}
   *
   * @param oldId an identifier of message to update.
   * @param newMessage new message object.
   */
  public void update(String oldId, MESSAGE newMessage) {
    int position = getMessagePositionById(oldId);
    if (position >= 0) {
      Wrapper<MESSAGE> element = new Wrapper<>(newMessage);
      items.set(position, element);
      notifyItemChanged(position);
    }
  }

  /**
   * Deletes message.
   *
   * @param message message to delete.
   */
  public void delete(MESSAGE message) {
    deleteById(message.getId());
  }

  /**
   * Deletes messages list.
   *
   * @param messages messages list to delete.
   */
  public void delete(List<MESSAGE> messages) {
    for (MESSAGE message : messages) {
      int index = getMessagePositionById(message.getId());
      items.remove(index);
      notifyItemRemoved(index);
    }
    recountDateHeaders();
  }

  /**
   * Deletes message by its identifier.
   *
   * @param id identifier of message to delete.
   */
  public void deleteById(String id) {
    int index = getMessagePositionById(id);
    if (index >= 0) {
      items.remove(index);
      notifyItemRemoved(index);
      recountDateHeaders();
    }
  }

  /**
   * Deletes messages by its identifiers.
   *
   * @param ids array of identifiers of messages to delete.
   */
  public void deleteByIds(String[] ids) {
    for (String id : ids) {
      int index = getMessagePositionById(id);
      items.remove(index);
      notifyItemRemoved(index);
    }
    recountDateHeaders();
  }

  /**
   * Clears the messages list.
   */
  public void clear() {
    items.clear();
  }

  /**
   * Enables selection mode.
   *
   * @param selectionListener listener for selected items count. To get selected messages use
   * {@link
   * #getSelectedMessages()}.
   */
  public void enableSelectionMode(SelectionListener selectionListener) {
    if (selectionListener == null) {
      throw new IllegalArgumentException(
          "SelectionListener must not be null. Use `disableSelectionMode()` if you want tp disable selection mode");
    } else {
      this.selectionListener = selectionListener;
    }
  }

  /**
   * Disables selection mode and removes {@link SelectionListener}.
   */
  public void disableSelectionMode() {
    this.selectionListener = null;
    unselectAllItems();
  }

  /**
   * Returns the list of selected messages.
   *
   * @return list of selected messages. Empty list if nothing was selected or selection mode is
   * disabled.
   */
  @SuppressWarnings("unchecked") public ArrayList<MESSAGE> getSelectedMessages() {
    ArrayList<MESSAGE> selectedMessages = new ArrayList<>();
    for (Wrapper wrapper : items) {
      if (wrapper.item instanceof IMessage && wrapper.isSelected) {
        selectedMessages.add((MESSAGE) wrapper.item);
      }
    }
    return selectedMessages;
  }

  /**
   * Unselect all of the selected messages. Notifies {@link SelectionListener} with zero count.
   */
  public void unselectAllItems() {
    for (int i = 0; i < items.size(); i++) {
      Wrapper wrapper = items.get(i);
      if (wrapper.isSelected) {
        wrapper.isSelected = false;
        notifyItemChanged(i);
      }
    }
    isSelectMode = false;
    selectedItemsCount = 0;
    notifySelectionChanged();
  }

  /**
   * Deletes all of the selected messages and disables selection mode.
   * Call {@link #getSelectedMessages()} before calling this method to delete messages from your
   * data source.
   */
  public void deleteSelectedMessages() {
    List<MESSAGE> selectedMessages = getSelectedMessages();
    delete(selectedMessages);
    unselectAllItems();
  }

  /**
   * Sets click listener for item. Fires ONLY if list is not in selection mode.
   *
   * @param onMessageClickListener click listener.
   */
  public void setOnMessageClickListener(OnMessageClickListener<MESSAGE> onMessageClickListener) {
    this.onMessageClickListener = onMessageClickListener;
  }

  /**
   * Sets long click listener for item. Fires only if selection mode is disabled.
   *
   * @param onMessageLongClickListener long click listener.
   */
  public void setOnMessageLongClickListener(
      OnMessageLongClickListener<MESSAGE> onMessageLongClickListener) {
    this.onMessageLongClickListener = onMessageLongClickListener;
  }

  /**
   * Set callback to be invoked when list scrolled to top.
   *
   * @param loadMoreListener listener.
   */
  public void setLoadMoreListener(OnLoadMoreListener loadMoreListener) {
    this.loadMoreListener = loadMoreListener;
  }

  /*
  * PRIVATE METHODS
  * */
  private void recountDateHeaders() {
    List<Integer> indicesToDelete = new ArrayList<>();

    for (int i = 0; i < items.size(); i++) {
      Wrapper wrapper = items.get(i);
      if (wrapper.item instanceof Date) {
        if (i == 0) {
          indicesToDelete.add(i);
        } else {
          if (items.get(i - 1).item instanceof Date) {
            indicesToDelete.add(i);
          }
        }
      }
    }

    Collections.reverse(indicesToDelete);
    for (int i : indicesToDelete) {
      items.remove(i);
      notifyItemRemoved(i);
    }
  }

  private void generateDateHeaders(List<MESSAGE> messages) {
    for (int i = 0; i < messages.size(); i++) {
      MESSAGE message = messages.get(i);
      this.items.add(new Wrapper<>(message));
      if (messages.size() > i + 1) {
        MESSAGE nextMessage = messages.get(i + 1);
        if (!DateFormatter.isSameDay(message.getCreatedAt(), nextMessage.getCreatedAt())) {
          this.items.add(new Wrapper<>(message.getCreatedAt()));
        }
      } else {
        this.items.add(new Wrapper<>(message.getCreatedAt()));
      }
    }
  }

  @SuppressWarnings("unchecked") private int getMessagePositionById(String id) {
    for (int i = 0; i < items.size(); i++) {
      Wrapper wrapper = items.get(i);
      if (wrapper.item instanceof IMessage) {
        MESSAGE message = (MESSAGE) wrapper.item;
        if (message.getId().contentEquals(id)) {
          return i;
        }
      }
    }
    return -1;
  }

  @SuppressWarnings("unchecked")
  private boolean isPreviousSameDate(int position, Date dateToCompare) {
    if (items.size() <= position) return false;
    if (items.get(position).item instanceof IMessage) {
      Date previousPositionDate = ((MESSAGE) items.get(position).item).getCreatedAt();
      return DateFormatter.isSameDay(dateToCompare, previousPositionDate);
    } else {
      return false;
    }
  }

  @SuppressWarnings("unchecked") private boolean isPreviousSameAuthor(String id, int position) {
    int prevPosition = position + 1;
    if (items.size() <= prevPosition) return false;

    if (items.get(prevPosition).item instanceof IMessage) {
      return ((MESSAGE) items.get(prevPosition).item).getUser().getId().contentEquals(id);
    } else {
      return false;
    }
  }

  private void incrementSelectedItemsCount() {
    selectedItemsCount++;
    notifySelectionChanged();
  }

  private void decrementSelectedItemsCount() {
    selectedItemsCount--;
    isSelectMode = selectedItemsCount > 0;

    notifySelectionChanged();
  }

  private void notifySelectionChanged() {
    if (selectionListener != null) {
      selectionListener.onSelectionChanged(selectedItemsCount);
    }
  }

  private void notifyMessageClicked(MESSAGE message) {
    if (onMessageClickListener != null) {
      onMessageClickListener.onMessageClick(message);
    }
  }

  private void notifyMessageLongClicked(MESSAGE message) {
    if (onMessageLongClickListener != null) {
      onMessageLongClickListener.onMessageLongClick(message);
    }
  }

  private View.OnClickListener getMessageClickListener(final Wrapper<MESSAGE> wrapper) {
    return new View.OnClickListener() {
      @Override public void onClick(View view) {
        if (selectionListener != null && isSelectMode) {
          wrapper.isSelected = !wrapper.isSelected;

          if (wrapper.isSelected) {
            incrementSelectedItemsCount();
          } else {
            decrementSelectedItemsCount();
          }

          MESSAGE message = (wrapper.item);
          notifyItemChanged(getMessagePositionById(message.getId()));
        } else {
          notifyMessageClicked(wrapper.item);
        }
      }
    };
  }

  private View.OnLongClickListener getMessageLongClickListener(final Wrapper<MESSAGE> wrapper) {
    return new View.OnLongClickListener() {
      @Override public boolean onLongClick(View view) {
        if (selectionListener == null) {
          notifyMessageLongClicked(wrapper.item);
          return true;
        } else {
          isSelectMode = true;
          view.callOnClick();
          return true;
        }
      }
    };
  }

  void setLayoutManager(RecyclerView.LayoutManager layoutManager) {
    this.layoutManager = layoutManager;
  }

    /*
    * LISTENERS
    * */

  void setStyle(MessagesListStyle style) {
    this.messagesListStyle = style;
  }

  /**
   * Interface definition for a callback to be invoked when next part of messages need to be loaded.
   */
  public interface OnLoadMoreListener {

    /**
     * Fires when user scrolled to the end of list.
     *
     * @param page next page to download.
     * @param totalItemsCount current items count.
     */
    void onLoadMore(int page, int totalItemsCount);
  }

  /**
   * Interface definition for a callback to be invoked when selected messages count is changed.
   */
  public interface SelectionListener {

    /**
     * Fires when selected items count is changed.
     *
     * @param count count of selected items.
     */
    void onSelectionChanged(int count);
  }

  /**
   * Interface definition for a callback to be invoked when message item is clicked.
   */
  public interface OnMessageClickListener<MESSAGE extends IMessage> {

    /**
     * Fires when message was clicked.
     *
     * @param message clicked message.
     */
    void onMessageClick(MESSAGE message);
  }

    /*
    * HOLDERS CONFIG
    * */

  /**
   * Interface definition for a callback to be invoked when message item is long clicked.
   */
  public interface OnMessageLongClickListener<MESSAGE extends IMessage> {

    /**
     * Fires when message was long clicked.
     *
     * @param message clicked message.
     */
    void onMessageLongClick(MESSAGE message);
  }

    /*
    * HOLDERS
    * */

  interface DefaultMessageViewHolder {
    void applyStyle(MessagesListStyle style);
  }

  public static class HoldersConfig {

    private Class<? extends BaseMessageViewHolder<? extends IMessage>> incomingHolder;
    private int incomingLayout;

    private Class<? extends BaseMessageViewHolder<? extends IMessage>> outcomingHolder;
    private int outcomingLayout;

    private Class<? extends ViewHolder<Date>> dateHeaderHolder;
    private int dateHeaderLayout;

    public HoldersConfig() {
      this.incomingHolder = DefaultIncomingMessageViewHolder.class;
      this.incomingLayout = R.layout.item_incoming_msg;

      this.outcomingHolder = DefaultOutcomingMessageViewHolder.class;
      this.outcomingLayout = R.layout.item_outcoming_msg;

      this.dateHeaderHolder = DefaultDateHeaderViewHolder.class;
      this.dateHeaderLayout = R.layout.item_date_header;
    }

    /**
     * Sets both of custom view holder class and layout resource for incoming message.
     *
     * @param holder holder class.
     * @param layout layout resource.
     */
    public void setIncoming(Class<? extends BaseMessageViewHolder<? extends IMessage>> holder,
        @LayoutRes int layout) {
      this.incomingHolder = holder;
      this.incomingLayout = layout;
    }

    /**
     * Sets custom view holder class for incoming message.
     *
     * @param holder holder class.
     */
    public void setIncomingHolder(
        Class<? extends BaseMessageViewHolder<? extends IMessage>> holder) {
      this.incomingHolder = holder;
    }

    /**
     * Sets custom layout resource for incoming message.
     *
     * @param layout layout resource.
     */
    public void setIncomingLayout(@LayoutRes int layout) {
      this.incomingLayout = layout;
    }

    /**
     * Sets both of custom view holder class and layout resource for incoming message.
     *
     * @param holder holder class.
     * @param layout layout resource.
     */
    public void setOutcoming(Class<? extends BaseMessageViewHolder<? extends IMessage>> holder,
        @LayoutRes int layout) {
      this.outcomingHolder = holder;
      this.outcomingLayout = layout;
    }

    /**
     * Sets custom view holder class for outcoming message.
     *
     * @param holder holder class.
     */
    public void setOutcomingHolder(
        Class<? extends BaseMessageViewHolder<? extends IMessage>> holder) {
      this.outcomingHolder = holder;
    }

    /**
     * Sets custom layout resource for outcoming message.
     *
     * @param layout layout resource.
     */
    public void setOutcomingLayout(@LayoutRes int layout) {
      this.outcomingLayout = layout;
    }

    /*
     * Sets both of custom view holder class and layout resource for date header.
     *
     * @param holder holder class.
     * @param layout layout resource.
     */
    public void setDateHeader(Class<? extends ViewHolder<Date>> holder, @LayoutRes int layout) {
      this.dateHeaderHolder = holder;
      this.dateHeaderLayout = layout;
    }

    /**
     * Sets custom view holder class for date header.
     *
     * @param holder holder class.
     */
    public void setDateHeaderHolder(Class<? extends ViewHolder<Date>> holder) {
      this.dateHeaderHolder = holder;
    }

    /**
     * Sets custom layout reource for date header.
     *
     * @param layout layout resource.
     */
    public void setDateHeaderLayout(@LayoutRes int layout) {
      this.dateHeaderLayout = layout;
    }
  }

  /**
   * The base class for view holders for incoming and outcoming message.
   * You can extend it to create your own holder in conjuction with custom layout or even using
   * default layout.
   */
  public static abstract class BaseMessageViewHolder<MESSAGE extends IMessage>
      extends ViewHolder<MESSAGE> {

    /**
     * Callback for implementing images loading in message list
     */
    protected ImageLoader imageLoader;
    private boolean isSelected;

    public BaseMessageViewHolder(View itemView) {
      super(itemView);
    }

    /**
     * Make message unselected
     *
     * @return weather is item selected.
     */
    public boolean isSelected() {
      return isSelected;
    }

    /**
     * Getter for {@link #imageLoader}
     *
     * @return image loader interface.
     */
    public ImageLoader getImageLoader() {
      return imageLoader;
    }
  }

  /**
   * Default view holder implementation for incoming message
   */
  public static class IncomingMessageViewHolder<MESSAGE extends IMessage>
      extends BaseMessageViewHolder<MESSAGE> implements DefaultMessageViewHolder {

    protected ViewGroup bubble;
    protected TextView text;
    protected TextView time;
    protected ImageView userAvatar;
    protected ImageView sticker;

    public IncomingMessageViewHolder(View itemView) {
      super(itemView);
      bubble = (ViewGroup) itemView.findViewById(R.id.bubble);
      text = (TextView) itemView.findViewById(R.id.messageText);
      time = (TextView) itemView.findViewById(R.id.messageTime);
      userAvatar = (ImageView) itemView.findViewById(R.id.messageUserAvatar);
      sticker = (ImageView) itemView.findViewById(R.id.messageSticker);
      int a = 5;
    }

    @Override public void onBind(MESSAGE message) {
      bubble.setSelected(isSelected());
      text.setText(message.getText());
      time.setText(DateFormatter.format(message.getCreatedAt(), DateFormatter.Template.TIME));

      boolean isAvatarExists =
          imageLoader != null && message.getUser().getAvatar() != null && !message.getUser()
              .getAvatar()
              .isEmpty();
      userAvatar.setVisibility(isAvatarExists ? View.VISIBLE : View.GONE);
      if (isAvatarExists) {
        if (!message.getText().contains(Constants.STICKER)) {
          imageLoader.loadImage(userAvatar, message.getUser().getAvatar());
        }
      }

      if (checkIfStickerEncode(message.getText())) {
        text.setVisibility(View.GONE);
        bubble.setVisibility(View.GONE);
        sticker.setVisibility(View.VISIBLE);
        Picasso.with(context).load(getStickerPath(message.getText())).into(sticker);
      } else {
        if (sticker != null) sticker.setVisibility(View.GONE);
        text.setVisibility(View.VISIBLE);
        bubble.setVisibility(View.VISIBLE);
      }
    }

    @Override public void applyStyle(MessagesListStyle style) {
      bubble.setPadding(style.getIncomingDefaultBubblePaddingLeft(),
          style.getIncomingDefaultBubblePaddingTop(), style.getIncomingDefaultBubblePaddingRight(),
          style.getIncomingDefaultBubblePaddingBottom());
      bubble.setBackground(style.getIncomingBubbleDrawable());

      text.setTextColor(style.getIncomingTextColor());
      text.setTextSize(TypedValue.COMPLEX_UNIT_PX, style.getIncomingTextSize());

      userAvatar.getLayoutParams().width = style.getIncomingAvatarWidth();
      userAvatar.getLayoutParams().height = style.getIncomingAvatarHeight();

      time.setTextColor(style.getIncomingTimeTextColor());
      time.setTextSize(TypedValue.COMPLEX_UNIT_PX, style.getIncomingTimeTextSize());
    }
  }

  private static class DefaultIncomingMessageViewHolder
      extends IncomingMessageViewHolder<IMessage> {

    public DefaultIncomingMessageViewHolder(View itemView) {
      super(itemView);
    }
  }

  /**
   * Default view holder implementation for outcoming message
   */
  public static class OutcomingMessageViewHolder<MESSAGE extends IMessage>
      extends BaseMessageViewHolder<MESSAGE> implements DefaultMessageViewHolder {

    protected ViewGroup bubble;
    protected TextView text;
    protected TextView time;
    protected ImageView userAvatar;
    protected ImageView sticker;

    public OutcomingMessageViewHolder(View itemView) {
      super(itemView);
      bubble = (ViewGroup) itemView.findViewById(R.id.bubble);
      text = (TextView) itemView.findViewById(R.id.messageText);
      time = (TextView) itemView.findViewById(R.id.messageTime);
      userAvatar = (ImageView) itemView.findViewById(R.id.messageUserAvatar);
      sticker = (ImageView) itemView.findViewById(R.id.messageSticker);
      int a = 5;
    }

    @Override public void onBind(MESSAGE message) {
      bubble.setSelected(isSelected());
      text.setText(message.getText());
      time.setText(DateFormatter.format(message.getCreatedAt(), DateFormatter.Template.TIME));

      if (userAvatar != null) {
        boolean isAvatarExists =
            message.getUser().getAvatar() != null && !message.getUser().getAvatar().isEmpty();
        userAvatar.setVisibility(isAvatarExists ? View.VISIBLE : View.GONE);
        if (isAvatarExists && imageLoader != null) {
          imageLoader.loadImage(userAvatar, message.getUser().getAvatar());
        }
      }

      if (checkIfStickerEncode(message.getText())) {
        if (sticker != null) {
          text.setVisibility(View.GONE);
          bubble.setVisibility(View.GONE);
          sticker.setVisibility(View.VISIBLE);
          Picasso.with(context).load(getStickerPath(message.getText())).into(sticker);
        }
      } else {
        if (sticker != null) sticker.setVisibility(View.GONE);
        text.setVisibility(View.VISIBLE);
        bubble.setVisibility(View.VISIBLE);
      }
    }

    @Override public void applyStyle(MessagesListStyle style) {
      bubble.setPadding(style.getOutcomingDefaultBubblePaddingLeft(),
          style.getOutcomingDefaultBubblePaddingTop(),
          style.getOutcomingDefaultBubblePaddingRight(),
          style.getOutcomingDefaultBubblePaddingBottom());
      bubble.setBackground(style.getOutcomingBubbleDrawable());

      text.setTextColor(style.getOutcomingTextColor());
      text.setTextSize(TypedValue.COMPLEX_UNIT_PX, style.getOutcomingTextSize());

      time.setTextColor(style.getOutcomingTimeTextColor());
      time.setTextSize(TypedValue.COMPLEX_UNIT_PX, style.getOutcomingTimeTextSize());
    }
  }

  private static class DefaultOutcomingMessageViewHolder
      extends OutcomingMessageViewHolder<IMessage> {

    public DefaultOutcomingMessageViewHolder(View itemView) {
      super(itemView);
    }
  }

  /**
   * Default view holder implementation for date header
   */
  public static class DefaultDateHeaderViewHolder extends ViewHolder<Date>
      implements DefaultMessageViewHolder {

    protected TextView text;
    protected String dateFormat;

    public DefaultDateHeaderViewHolder(View itemView) {
      super(itemView);
      text = (TextView) itemView.findViewById(R.id.messageText);
    }

    @Override public void onBind(Date date) {
      text.setText(DateFormatter.format(date, dateFormat));
    }

    @Override public void applyStyle(MessagesListStyle style) {
      text.setTextSize(TypedValue.COMPLEX_UNIT_PX, style.getDateHeaderTextSize());
      text.setTextColor(style.getDateHeaderTextColor());
      text.setPadding(style.getDateHeaderPadding(), style.getDateHeaderPadding(),
          style.getDateHeaderPadding(), style.getDateHeaderPadding());
      dateFormat = style.getDateHeaderFormat();
      dateFormat = dateFormat == null ? DateFormatter.Template.STRING_MONTH.get() : dateFormat;
    }
  }

  /*
  * WRAPPER
  * */
  private class Wrapper<DATA> {
    boolean isSelected;
    private DATA item;

    Wrapper(DATA item) {
      this.item = item;
    }
  }
}

