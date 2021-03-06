package com.vpaliy.bakingapp.ui.activity;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import com.vpaliy.bakingapp.BakingApp;
import com.vpaliy.bakingapp.R;
import com.vpaliy.bakingapp.mvp.contract.RecipeStepsContract.Presenter;
import com.vpaliy.bakingapp.mvp.presenter.StepsPresenter;
import com.vpaliy.bakingapp.mvp.presenter.StepsPresenter.StepsWrapper;
import com.vpaliy.bakingapp.ui.bus.event.OnChangeToolbarEvent;
import com.vpaliy.bakingapp.ui.bus.event.OnChangeVisibilityEvent;
import com.vpaliy.bakingapp.ui.bus.event.OnMoveToStepEvent;
import com.vpaliy.bakingapp.ui.bus.event.OnStepClickEvent;
import com.vpaliy.bakingapp.ui.fragment.StepsFragment;
import com.vpaliy.bakingapp.ui.fragment.SummaryFragment;
import com.vpaliy.bakingapp.utils.Constants;
import com.vpaliy.bakingapp.utils.Permissions;
import com.vpaliy.bakingapp.utils.messenger.Messenger;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.support.v4.app.FragmentManager;
import android.view.View;
import butterknife.ButterKnife;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import butterknife.BindBool;
import butterknife.BindView;

public class DetailsActivity extends BaseActivity {

    @BindView(R.id.action_bar)
    protected Toolbar actionBar;

    @BindBool(R.bool.is_tablet)
    protected boolean isTablet;

    private Presenter stepsPresenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_details);
        ButterKnife.bind(this);
        setActionBar();
        if(savedInstanceState==null){
            setUI(getIntent().getExtras());
        }
    }

    private void setUI(Bundle args){
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.recipe, SummaryFragment.newInstance(args), Constants.SUMMARY_TAG)
                .commit();
    }

    private void setActionBar(){
        setSupportActionBar(actionBar);
        if(getSupportActionBar()!=null){
            actionBar.setTitleTextColor(ContextCompat.getColor(this,R.color.colorText));
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            Drawable drawable=actionBar.getNavigationIcon();
            actionBar.setNavigationOnClickListener(v->onBackPressed());
            if(drawable!=null){
                drawable.setColorFilter(ContextCompat.getColor(this,R.color.colorText), PorterDuff.Mode.SRC_ATOP);
            }
        }
    }

    @Override
    void handleEvent(@NonNull Object event) {
        if(event instanceof OnStepClickEvent){
            showSteps(OnStepClickEvent.class.cast(event));
        }else if(event instanceof OnChangeToolbarEvent){
            changeToolbar(OnChangeToolbarEvent.class.cast(event));
        }else if(event instanceof OnChangeVisibilityEvent){
            changeVisibility(OnChangeVisibilityEvent.class.cast(event));
        }else if(event instanceof OnMoveToStepEvent){
            moveToStep(OnMoveToStepEvent.class.cast(event));
        }
    }

    private void moveToStep(OnMoveToStepEvent event){
        SummaryFragment fragment=SummaryFragment.class.cast
                (getSupportFragmentManager().findFragmentByTag(Constants.SUMMARY_TAG));
        if(fragment!=null){
            fragment.highlightStep(event.step);
        }
    }

    private void changeToolbar(OnChangeToolbarEvent event){
        actionBar.setTitle(event.text);
    }

    private void changeVisibility(OnChangeVisibilityEvent event){
        if(!event.visible){
            if(Permissions.checkForVersion(19)){
                View decorView = getWindow().getDecorView();
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        }else{
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(0);
        }
    }

    private void showSteps(OnStepClickEvent clickEvent){
        if(!isTablet){
            stepsSeparately(clickEvent);
        }else{
            updateSteps(clickEvent);
        }
    }

    private void changeActionBarVisibility(boolean isVisible){
        if(getSupportActionBar()!=null){
            if(!isVisible) {
                getSupportActionBar().hide();
            }else{
                getSupportActionBar().show();
            }
        }
    }
    private void stepsSeparately(OnStepClickEvent clickEvent){
        changeActionBarVisibility(false);
        FragmentManager manager=getSupportFragmentManager();
        if(manager.findFragmentByTag(Constants.STEPS_TAG)!=null){
            manager.beginTransaction()
                    .remove(manager.findFragmentByTag(Constants.SUMMARY_TAG))
                    .show(manager.findFragmentByTag(Constants.STEPS_TAG))
                    .commit();
        }else{
            StepsWrapper wrapper=StepsWrapper.wrap(clickEvent.steps,clickEvent.currentStep);
            stepsPresenter=new StepsPresenter(wrapper,new Messenger(this));
            StepsFragment fragment=new StepsFragment();
            fragment.attachPresenter(stepsPresenter);
            manager.beginTransaction()
                    .addToBackStack(Constants.SUMMARY_TAG)
                    .replace(R.id.recipe,fragment,Constants.STEPS_TAG)
                    .commit();
        }
    }

    private void updateSteps(OnStepClickEvent clickEvent){
        if(stepsPresenter==null){
            StepsWrapper wrapper=StepsWrapper.wrap(clickEvent.steps,clickEvent.currentStep);
            stepsPresenter=new StepsPresenter(wrapper,new Messenger(this));
            StepsFragment fragment=StepsFragment.class.cast(
                    getSupportFragmentManager().findFragmentById(R.id.recipe_steps));
            fragment.attachPresenter(stepsPresenter);
            stepsPresenter.showCurrent();
        }else{
            stepsPresenter.requestStep(clickEvent.currentStep);
        }
    }

    @Override
    public void onBackPressed() {
        if(!isTablet) {
            changeActionBarVisibility(true);
            FragmentManager manager = getSupportFragmentManager();
            if (manager.getBackStackEntryCount() > 0) {
                manager.popBackStack();
                manager.beginTransaction()
                        .show(manager.findFragmentByTag(Constants.SUMMARY_TAG))
                        .commit();
                if(getSupportActionBar()!=null){
                    if(!getSupportActionBar().isShowing()){
                        changeVisibility(OnChangeVisibilityEvent.change(true));
                    }
                }
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    void initializeDependencies() {
        BakingApp.appInstance()
                .appComponent()
                .inject(this);
    }


}
