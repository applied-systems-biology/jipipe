class CellposeCustom():
    def __init__(self, gpu=False, pretrained_model=None, diam_mean=None, pretrained_size=None, net_avg=True, device=None, torch=True):
        super(CellposeCustom, self).__init__()
        from cellpose.core import UnetModel, assign_device, check_mkl, use_gpu, parse_model_string
        from cellpose.models import CellposeModel, SizeModel
        self.torch = True
        torch_str = ['','torch'][self.torch]

        # assign device (GPU or CPU)
        sdevice, gpu = assign_device(self.torch, gpu)
        self.device = device if device is not None else sdevice
        self.gpu = gpu
        self.pretrained_model = pretrained_model
        self.pretrained_size = pretrained_size
        self.diam_mean = diam_mean

        if not net_avg:
            self.pretrained_model = self.pretrained_model[0]

        self.cp = CellposeModel(device=self.device, gpu=self.gpu,
                                pretrained_model=self.pretrained_model,
                                diam_mean=self.diam_mean)
        if pretrained_size is not None:
            self.sz = SizeModel(device=self.device, pretrained_size=self.pretrained_size,
                                cp_model=self.cp)
        else:
            self.sz = None

    def eval(self, x, batch_size=8, channels=None, channel_axis=None, z_axis=None,
             invert=False, normalize=True, diameter=30., do_3D=False, anisotropy=None,
             net_avg=True, augment=False, tile=True, tile_overlap=0.1, resample=False, interp=True,
             flow_threshold=0.4, cellprob_threshold=0.0, min_size=15,
             stitch_threshold=0.0, rescale=None, progress=None):
        from cellpose.models import models_logger
        tic0 = time.time()

        estimate_size = True if (diameter is None or diameter==0) else False
        models_logger.info('Estimate size: ' + str(estimate_size))
        if estimate_size and self.pretrained_size is not None and not do_3D and x[0].ndim < 4:
            tic = time.time()
            models_logger.info('~~~ ESTIMATING CELL DIAMETER(S) ~~~')
            diams, _ = self.sz.eval(x, channels=channels, channel_axis=channel_axis, invert=invert, batch_size=batch_size,
                                    augment=augment, tile=tile)
            rescale = self.diam_mean / np.array(diams)
            diameter = None
            models_logger.info('estimated cell diameter(s) in %0.2f sec'%(time.time()-tic))
            models_logger.info('>>> diameter(s) = ')
            if isinstance(diams, list) or isinstance(diams, np.ndarray):
                diam_string = '[' + ''.join(['%0.2f, '%d for d in diams]) + ']'
            else:
                diam_string = '[ %0.2f ]'%diams
            models_logger.info(diam_string)
        elif estimate_size:
            if self.pretrained_size is None:
                reason = 'no pretrained size model specified in model Cellpose'
            else:
                reason = 'does not work on non-2D images'
            models_logger.warning(f'could not estimate diameter, {reason}')
            diams = self.diam_mean
        else:
            diams = diameter

        tic = time.time()
        models_logger.info('~~~ FINDING MASKS ~~~')
        masks, flows, styles = self.cp.eval(x,
                                            batch_size=batch_size,
                                            invert=invert,
                                            diameter=diameter,
                                            rescale=rescale,
                                            anisotropy=anisotropy,
                                            channels=channels,
                                            channel_axis=channel_axis,
                                            z_axis=z_axis,
                                            augment=augment,
                                            tile=tile,
                                            do_3D=do_3D,
                                            net_avg=net_avg,
                                            progress=progress,
                                            tile_overlap=tile_overlap,
                                            resample=resample,
                                            interp=interp,
                                            flow_threshold=flow_threshold,
                                            cellprob_threshold=cellprob_threshold,
                                            min_size=min_size,
                                            stitch_threshold=stitch_threshold)
        models_logger.info('>>>> TOTAL TIME %0.2f sec'%(time.time()-tic0))

        return masks, flows, styles, diams