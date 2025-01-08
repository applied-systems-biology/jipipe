/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.registration.simple;

import bunwarpj.trakem2.transform.CubicBSplineTransform;
import mpicbg.trakem2.transform.*;

/**
 * Feature extraction model for the registration nodes
 */
public enum SimpleImageRegistrationModel {
    Translation,
    Rigid,
    Similarity,
    Affine,
    Elastic,
    MovingLeastSquares;

    public CoordinateTransform toCoordinateTransform()
    {
        CoordinateTransform t;
        switch (this)
        {
            case Translation: t = new TranslationModel2D(); break;
            case Rigid: t = new RigidModel2D(); break;
            case Similarity: t = new SimilarityModel2D(); break;
            case Affine: t = new AffineModel2D(); break;
            case Elastic: t = new CubicBSplineTransform(); break;
            case MovingLeastSquares: t = new MovingLeastSquaresTransform(); break;
            default:
                throw new RuntimeException("Unknown image registration model: " + this);
        }
        return t;
    }
}
