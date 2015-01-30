package android.app.printerapp.library;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.printerapp.ListContent;
import android.app.printerapp.MainActivity;
import android.app.printerapp.R;
import android.app.printerapp.devices.DevicesListController;
import android.app.printerapp.devices.database.DatabaseController;
import android.app.printerapp.library.detail.DetailViewFragment;
import android.app.printerapp.model.ModelFile;
import android.app.printerapp.model.ModelPrinter;
import android.app.printerapp.octoprint.OctoprintFiles;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;

/**
 * This class will handle the click events for the library elements
 *
 * @author alberto-baeza
 */
public class LibraryOnClickListener implements OnItemClickListener, OnItemLongClickListener {

    private LibraryFragment mContext;
    private ListView mListView;
    private ActionMode mActionMode;

    public LibraryOnClickListener(LibraryFragment context, ListView list) {
        this.mContext = context;
        this.mListView = list;
    }

    //On long click we'll display the gcodes
    public void onOverflowButtonClick(View view, int position) {

        //Avoid to click in the header
//        position--;

        Log.d("LibraryOnClickListener", "onOverflowButtonClick");

        File f = LibraryController.getFileList().get(position);

        //If it's not IN the printer
        if ((!f.getParent().contains("printer")) &&
                (!f.getParent().contains("sd")) &&
                (!f.getParent().contains("local"))) {

            showOptionPopUp(view, position);
        }
    }

    @SuppressLint("SdCardPath")
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

        //Avoid to click in the header
//        arg2--;

        if (mListView.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE){

            boolean checked = mListView.isItemChecked(arg2);

            mListView.setItemChecked(arg2, checked);

            LibraryAdapter listAdapter = (LibraryAdapter) mListView.getAdapter();
            listAdapter.setItemChecked(arg2, checked);

            mContext.notifyAdapter();


        }else {

            Log.d("LibraryOnClickListener", "onItemClick");

            //Logic for getting file type
            File f = LibraryController.getFileList().get(arg2);

            //If it's folder open it
            if (f.isDirectory()) {


                //If it's project folder, send stl
                if (LibraryController.isProject(f)) {
                    //Show detail view as a fragment
                    showRightPanel(arg2);

                } else {

                    //Not a project, open folder
                    String folderName = f.getName();
                    LibraryController.reloadFiles(f.getAbsolutePath());

                    mContext.showListHeader(folderName);

                    mContext.sortAdapter();


                }

                //If it's not a folder, just send the file
            } else {

                //it's a printer file
                if (f.getParent().contains("printer")) {
                    LibraryController.retrievePrinterFiles(Long.parseLong(f.getName()));
                    mContext.notifyAdapter();

                } else {

                    try {

                        ModelPrinter p = DevicesListController.getPrinter(Long.parseLong(LibraryController.getCurrentPath().getName()));

                        //it's a printer folder because there's a printer with the same name
                        if (p != null) {
                            //either sd or internal
                            if (f.getParent().equals("sd")) {
                                OctoprintFiles.fileCommand(mContext.getActivity(), p.getAddress(), f.getName(), "/sdcard/", false, true);
                                //OctoprintSlicing.sliceCommand(mContext.getActivity(), p.getAddress(), f, "/local/");
                            } else
                                OctoprintFiles.fileCommand(mContext.getActivity(), p.getAddress(), f.getName(), "/local/", false, true);
                            Toast.makeText(mContext.getActivity(), "Loading " + f.getName() + " in " + p.getDisplayName(), Toast.LENGTH_LONG).show();
                        } else {

                            //it's a raw file
                            if (f.getAbsoluteFile().length() > 0) {
                                //TODO select printer for raw files?
                                //DevicesListController.selectPrinter(mContext.getActivity(), f , 0);
                                MainActivity.requestOpenFile(f.getAbsolutePath());

                            } else {
                                Toast.makeText(mContext.getActivity(), R.string.storage_toast_corrupted, Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (NumberFormatException e) {

                        e.printStackTrace();

                    }
                }
            }
        }


    }

    private void showRightPanel(final int index) {

        FragmentTransaction fragmentTransaction = mContext.getFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);

        //New DetailView with the file as an index
        DetailViewFragment detail = new DetailViewFragment();
        Bundle args = new Bundle();
        args.putInt("index", index);
        detail.setArguments(args);

        fragmentTransaction.replace(R.id.right_panel_container, detail, ListContent.ID_DETAIL).commit();
    }

    //Show dialog for handling files
    private void showOptionPopUp(View view, final int index) {

        //Creating the instance of PopupMenu
        final PopupMenu popup = new PopupMenu(mContext.getActivity(), view);

        //Logic for getting file type
        final File f = LibraryController.getFileList().get(index);

        //Different pop ups for different type of files
        if (f.getParent().equals("sd") || f.getParent().equals("local")) {
            popup.getMenuInflater().inflate(R.menu.library_model_menu_local, popup.getMenu());
        } else {
            popup.getMenuInflater().inflate(R.menu.library_model_menu, popup.getMenu());
        }


        //registering popup with OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.library_model_print: //Print / Multiprint
                        if (f.isDirectory()) {
                            if (LibraryController.isProject(f)) {
                                //Show detail view as a fragment
                                showRightPanel(index);
                            }
                        } else {
                            MainActivity.requestOpenFile(f.getAbsolutePath());

                        }
                        break;
                    case R.id.library_model_edit: //Edit
                        //TODO Doesn't work when empty gcodes comeon
                        popup.dismiss();
                        if (f.isDirectory()) {
                            if (LibraryController.isProject(f)) {

                                if (((ModelFile) f).getStl() == null) {
                                    MainActivity.requestOpenFile(((ModelFile) f).getGcodeList());
                                    //DevicesListController.selectPrinter(mContext.getActivity(), new File (((ModelFile)f).getGcodeList()) , 0);

                                } else {
                                    MainActivity.requestOpenFile(((ModelFile) f).getStl());

                                }
                            }
                        } else {
                            //Check if the gcode is empty, won't work if file is actually corrupted
                            if (f.getAbsoluteFile().length() > 0) {
                                MainActivity.requestOpenFile(f.getAbsolutePath());
                            } else {
                                Toast.makeText(mContext.getActivity(), R.string.storage_toast_corrupted, Toast.LENGTH_SHORT).show();
                            }
                        }
                        break;
                    case R.id.library_model_move: //Move
                        mContext.setMoveFile(f);
                        Toast.makeText(mContext.getActivity(), R.string.library_paste_toast, Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.library_model_delete: //Delete
                        AlertDialog.Builder adb_delete = new AlertDialog.Builder(mContext.getActivity());
                        adb_delete.setTitle(R.string.library_delete_dialog_title);
                        adb_delete.setMessage(f.getName());
                        adb_delete.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                LibraryController.deleteFiles(f);
                                LibraryController.getFileList().remove(f);

                                if (DatabaseController.isPreference(DatabaseController.TAG_FAVORITES, f.getName())) {
                                    DatabaseController.handlePreference(DatabaseController.TAG_FAVORITES, f.getName(), null, false);
                                }
                                mContext.refreshFiles();

                            }
                        });

                        adb_delete.setNegativeButton(R.string.cancel, null);
                        adb_delete.show();
                        break;
                }
                return true;
            }
        });

        popup.show();//showing popup menu

    }


    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {

        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mListView.setSelector(mContext.getResources().getDrawable(R.drawable.list_selector));

        LibraryAdapter listAdapter = (LibraryAdapter) mListView.getAdapter();
        listAdapter.setSelectionMode(true);

        if (mActionMode != null) {
            return false;
        }

        // Start the CAB using the ActionMode.Callback defined above
        mActionMode = ((ActionBarActivity)mContext.getActivity()).startSupportActionMode(mActionModeCallback);
//        view.setSelected(true);

        Log.i("OUT","LongCLICK");
        return false;
    }

    private void selectItemToDelete(int i){


    }

    /**
     * Action mode
     */

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.delete_menu, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {

                case R.id.library_menu_delete:

                    Log.i("DELETE","COUNT " + mListView.getCheckedItemCount());

                    final SparseBooleanArray ids = mListView.getCheckedItemPositions();

                    LayoutInflater inflater = (LayoutInflater) mContext.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View deleteDialogView = inflater.inflate(R.layout.dialog_delete_models, null);
                    ((TextView)deleteDialogView.findViewById(R.id.delete_files_info_textview)).setText(mContext.getResources().getQuantityString(R.plurals.library_models_delete, ids.size()));
                    ((ImageView)deleteDialogView.findViewById(R.id.delete_files_icon)).setColorFilter(mContext.getResources().getColor(R.color.body_text_2));

                    //TODO Set images
                    if(mListView.getCheckedItemCount() == 1) {

                         ((TextView)deleteDialogView.findViewById(R.id.files_num_textview)).setText(LibraryController.getFileList().get(ids.keyAt(0)).getName());
                    }
                    else {
                        ((TextView)deleteDialogView.findViewById(R.id.files_num_textview)).setText(String.format(mContext.getResources().getString(R.string.library_menu_models_delete_files), mListView.getCheckedItemCount()));
                    }

                    new MaterialDialog.Builder(mContext.getActivity())
                            .title(mContext.getResources().getQuantityString(R.plurals.library_models_delete_title, ids.size()))
                            .customView(deleteDialogView, true)
                            .positiveColorRes(R.color.theme_accent_1)
                            .positiveText(R.string.confirm)
                            .callback(new MaterialDialog.ButtonCallback() {

                                @Override
                                public void onNegative(MaterialDialog dialog) {
                                    super.onNegative(dialog);

                                    mActionMode.finish();

                                }

                                @Override
                                public void onPositive(MaterialDialog dialog) {

                                    for (int i = 0; i < ids.size(); i++) {


                                        if (ids.valueAt(i)) {

                                            File file = LibraryController.getFileList().get(ids.keyAt(i));

                                            Log.i("DELETE", "DELETE " + file.getName());


                                            LibraryController.deleteFiles(file);

                                        }
                                    }

                                    mActionMode.finish();
                                }
                            })
                            .negativeText(R.string.cancel)
                            .negativeColorRes(R.color.body_text_2)
                            .show();

            }

            return false;
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mListView.clearChoices();

            //Removed because was causing issues with checked items size
            /*for (int i = 0; i < mListView.getCount(); i++){
               mListView.setItemChecked(i, false);
            }*/


            mListView.post(new Runnable() {
                @Override
                public void run() {
                    mActionMode = null;
                    mListView.setSelector(R.drawable.list_selector);
                    mListView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
                    mContext.notifyAdapter();
                    LibraryAdapter listAdapter = (LibraryAdapter) mListView.getAdapter();
                    listAdapter.setSelectionMode(false);
                }
            });

            mContext.refreshFiles();
        }
    };
}
