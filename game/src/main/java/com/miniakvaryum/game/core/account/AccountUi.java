package com.miniakvaryum.game.core.account;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.miniakvaryum.domain.core.account.AccountService;

/**
 * What every account screen needs: before sign-in it comes from {@link AccountFlow}, inside the
 * game from {@link AccountScreens}.
 */
interface AccountUi {

    Skin skin();

    AccountService accounts();

    AccountPictures pictures();

    /** Text field look with some space inside. */
    TextField.TextFieldStyle fieldStyle();

    Color background();
}
