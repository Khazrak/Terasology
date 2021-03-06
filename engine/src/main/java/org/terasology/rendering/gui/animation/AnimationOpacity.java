/*
 * Copyright 2013 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.gui.animation;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.terasology.asset.Assets;
import org.terasology.rendering.assets.material.Material;
import org.terasology.rendering.assets.mesh.Mesh;
import org.terasology.rendering.opengl.DefaultRenderingProcess;
import org.terasology.rendering.primitives.Tessellator;
import org.terasology.rendering.primitives.TessellatorHelper;

import javax.vecmath.Vector4f;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glScalef;
import static org.lwjgl.opengl.GL11.glTranslatef;

public class AnimationOpacity extends Animation {
    private float toOpacity;
    private float fromOpacity;
    private float currentOpacity;
    private int factor;
    private float speed;
    private Mesh mesh;

    private DefaultRenderingProcess.FBO fbo;
    private String id;


    public AnimationOpacity(float fromOpacity, float toOpacity, float speed) {
        this.fromOpacity = fromOpacity;
        this.toOpacity = toOpacity;
        this.speed = speed;
        this.id = generateId();

        if (fromOpacity < toOpacity) {
            this.factor = 1;
        } else {
            this.factor = -1;
        }

        Tessellator tessellator = new Tessellator();
        TessellatorHelper.addGUIQuadMesh(tessellator, new Vector4f(255f / 256f, 255f / 256f, 255f / 256f, 1.0f), 1.0f, 1.0f);
        mesh = tessellator.generateMesh();
        currentOpacity = fromOpacity;
    }

    @Override
    public void renderBegin() {
        if (fbo == null) {
            fbo = DefaultRenderingProcess.getInstance().createFBO(id, Display.getWidth(), Display.getHeight(), DefaultRenderingProcess.FBOType.DEFAULT, true, false);
        } else if (fbo.height != Display.getHeight() || fbo.width != Display.getWidth()) {
            fbo = DefaultRenderingProcess.getInstance().createFBO(id, Display.getWidth(), Display.getHeight(), DefaultRenderingProcess.FBOType.DEFAULT, true, false);
        }

        DefaultRenderingProcess.getInstance().getFBO(id).bind();
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void renderEnd() {
        DefaultRenderingProcess.getInstance().getFBO(id).unbind();
        Material program = Assets.getMaterial("engine:animateOpacity");
        program.setFloat("alpha", currentOpacity, true);
        program.enable();
        DefaultRenderingProcess.getInstance().getFBO(id).bindTexture();
        glMatrixMode(GL_TEXTURE);
        glPushMatrix();
        glLoadIdentity();
        glTranslatef(0, 0.5f, 0);
        glScalef(1f, -1f, 1f);
        glTranslatef(target.getAbsolutePosition().x / Display.getWidth(), target.getAbsolutePosition().y / Display.getHeight() - 0.5f, 0f);
        glScalef(target.getSize().x / Display.getWidth(), target.getSize().y / Display.getHeight(), 1f);
        glMatrixMode(GL11.GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        glTranslatef(target.getAbsolutePosition().x, target.getAbsolutePosition().y, 0f);
        glScalef(target.getSize().x, target.getSize().y, 1.0f);
        mesh.render();
        glPopMatrix();
        glMatrixMode(GL_TEXTURE);
        glPopMatrix();
        glMatrixMode(GL11.GL_MODELVIEW);
    }

    @Override
    public void update() {
        if ((factor > 0 && currentOpacity < toOpacity) || (factor < 0 && currentOpacity > toOpacity)) {
            currentOpacity += factor * speed * 0.01f;

            if (!target.isVisible() && currentOpacity > 0.05f) {
                target.setVisible(true);
            }

        } else {
            if (!isRepeat()) {
                currentOpacity = toOpacity;

                if (currentOpacity <= 0) {
                    target.setVisible(false);
                }

                this.stop();
            } else {
                float tempOpacity = toOpacity;
                toOpacity = fromOpacity;
                fromOpacity = tempOpacity;
                currentOpacity = fromOpacity;
                factor *= -1;
            }

        }
    }

    private String generateId() {
        return "opacity" + this.hashCode();
    }

}
