#!/usr/bin/env python3
"""
Unified Cellpose data extraction script for cellpose2, cellpose3, and cellpose4 formats.
Extracts data and images from existing npy files with performance optimizations.
"""

import argparse
import os
import sys
import json
import time
import numpy as np
import cv2
import tifffile
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed
import multiprocessing

# Import Cellpose utilities (assuming same structure as existing scripts)
from cellpose import utils
from cellpose import io

def imsave(filename, arr):
    """
    Enhanced image saving with large TIFF support.
    """
    # Use tifffile for large TIFF support
    if isinstance(filename, str) and filename.lower().endswith('.tif'):
        # For TIFF files, use tifffile with bigtiff support
        tifffile.imwrite(filename, arr, bigtiff=True, compression='zlib')
    else:
        # For other formats, use cellpose io.imsave
        io.imsave(filename, arr)

def get_diameter(npy_data):
    """
    Get diameter field from cellpose data, supporting both v2 and v3.
    """
    data_dict = npy_data.item()
    
    # Check for cellpose3 format first (diameter)
    if 'diameter' in data_dict:
        return data_dict['diameter']
    # Check for cellpose2 format (est_diam)
    elif 'est_diam' in data_dict:
        return data_dict['est_diam']
    else:
        raise ValueError("Neither 'diameter' (cellpose3) nor 'est_diam' (cellpose2) field found in data")

def detect_cellpose_version(npy_data):
    """
    Automatically detect cellpose version by checking field names.
    Returns '2' for cellpose2 (est_diam) or '3'/'4' for cellpose3/cellpose4 (diameter).
    """
    data_dict = npy_data.item()
    
    if 'diameter' in data_dict:
        return '3'  # Cellpose 3 and 4 use the same format
    elif 'est_diam' in data_dict:
        return '2'
    else:
        raise ValueError("Cannot detect cellpose version - neither 'diameter' nor 'est_diam' field found")

def process_roi_optimized(npy_data):
    """
    Optimized ROI extraction using vectorized operations.
    """
    masks = npy_data.item().get("masks")
    if masks is None:
        return []
    
    roi_list = []
    
    if masks.ndim == 3:
        # 3D case - process each Z slice
        for z in range(masks.shape[0]):
            coords_list = utils.outlines_list(masks[z,:,:])
            # Use list comprehension for better performance
            roi_list.extend([dict(z=z, coords=[dict(x=int(x[0]), y=int(x[1])) for x in coords]) 
                           for coords in coords_list])
    else:
        # 2D case
        coords_list = utils.outlines_list(masks)
        roi_list.extend([dict(coords=[dict(x=int(x[0]), y=int(x[1])) for x in coords]) 
                       for coords in coords_list])
    
    return roi_list

def extract_data_from_npy(npy_file, output_dir, options, progress_callback=None):
    """
    Extract data from a single npy file with selective output generation.
    """
    start_time = time.time()
    npy_base_output_path = os.path.join(output_dir, os.path.splitext(os.path.basename(npy_file))[0])
    
    if progress_callback:
        progress_callback(f"Processing {os.path.basename(npy_file)}", 0)
    
    # Load the npy file
    npy_data = np.load(npy_file, allow_pickle=True)
    
    # Detect or use specified cellpose version
    if options.cellpose_version:
        cellpose_version = options.cellpose_version
    else:
        try:
            cellpose_version = detect_cellpose_version(npy_data)
        except ValueError as e:
            if progress_callback:
                progress_callback(f"Error: {str(e)}", -1)
            raise
    
    # Get available data keys
    data_dict = npy_data.item()
    available_keys = list(data_dict.keys())
    
    if progress_callback:
        progress_callback(f"Loaded {os.path.basename(npy_file)} - Available: {', '.join(available_keys)}", 25)
    
    # Extract probabilities if not skipped
    if not options.skip_probabilities and 'flows' in data_dict:
        try:
            flows = data_dict['flows']
            if len(flows) > 1:
                imsave(npy_base_output_path + "_probabilities.tif", flows[1])
                if progress_callback:
                    progress_callback("Extracted probabilities", 40)
        except Exception as e:
            if progress_callback:
                progress_callback(f"Warning: Failed to extract probabilities - {str(e)}", 40)
    
    # Extract flows if not skipped
    if not options.skip_flows and 'flows' in data_dict:
        flows = data_dict['flows']
        try:
            # RGB flows
            if not options.skip_flows_rgb and len(flows) > 0:
                imsave(npy_base_output_path + "_flows_rgb.tif", flows[0])
                if progress_callback:
                    progress_callback("Extracted RGB flows", 45)
            
            # Z flows
            if not options.skip_flows_z and len(flows) > 1:
                imsave(npy_base_output_path + "_flows_z.tif", flows[1])
                if progress_callback:
                    progress_callback("Extracted Z flows", 47)
            
            # dz_dy_dx flows
            if not options.skip_flows_dz_dy_dx and len(flows) > 4:
                imsave(npy_base_output_path + "_flows_dz_dy_dx.tif", flows[4])
                if progress_callback:
                    progress_callback("Extracted dz/dy/dx flows", 49)
            
            if progress_callback:
                progress_callback("Extracted flows", 50)
        except Exception as e:
            if progress_callback:
                progress_callback(f"Warning: Failed to extract flows - {str(e)}", 50)
    
    # Extract labels if not skipped
    if not options.skip_labels and 'masks' in data_dict:
        try:
            masks = data_dict['masks']
            if masks.dtype != np.short and masks.dtype != np.uint8:
                masks = masks.astype(np.float32)
            imsave(npy_base_output_path + "_labels.tif", masks)
            if progress_callback:
                progress_callback("Extracted labels", 60)
        except Exception as e:
            if progress_callback:
                progress_callback(f"Warning: Failed to extract labels - {str(e)}", 60)
    
    # Extract ROI if not skipped
    if not options.skip_roi and 'masks' in data_dict:
        try:
            roi_list = process_roi_optimized(npy_data)
            with open(npy_base_output_path + "_roi.json", "w") as f:
                json.dump(roi_list, f, indent=4)
            if progress_callback:
                progress_callback(f"Extracted {len(roi_list)} ROIs", 80)
        except Exception as e:
            if progress_callback:
                progress_callback(f"Warning: Failed to extract ROI - {str(e)}", 80)
    
    # Extract additional info if not skipped
    if not options.skip_info:
        try:
            json_data = {
                "chan_choose": data_dict.get("chan_choose"),
                "diameter": get_diameter(npy_data),
                "cellpose_version": cellpose_version
            }
            with open(npy_base_output_path + "_info.json", "w") as f:
                json.dump(json_data, f, indent=4)
            if progress_callback:
                progress_callback("Extracted metadata", 90)
        except Exception as e:
            if progress_callback:
                progress_callback(f"Warning: Failed to extract metadata - {str(e)}", 90)
    
    processing_time = time.time() - start_time
    if progress_callback:
        progress_callback(f"Completed {os.path.basename(npy_file)} in {processing_time:.2f}s", 100)
    
    return {
        'file': npy_file,
        'processing_time': processing_time,
        'cellpose_version': cellpose_version,
        'available_keys': available_keys
    }

def progress_callback_simple(message, percentage):
    """Simple progress callback for basic reporting."""
    if percentage >= 0:
        print(f"[{percentage:3d}%] {message}")
    else:
        print(f"[ERROR] {message}")

def progress_callback_detailed(message, percentage):
    """Detailed progress callback with ETA calculation."""
    current_time = time.time()
    if percentage >= 0 and percentage < 100:
        eta = "Calculating..."
        print(f"[{percentage:3d}%] {message} - ETA: {eta}")
    elif percentage == 100:
        print(f"[100%] {message} - COMPLETED")
    else:
        print(f"[ERROR] {message}")

class ProgressTracker:
    """Progress tracking for batch processing."""
    def __init__(self, total_files, verbose=False):
        self.total_files = total_files
        self.completed_files = 0
        self.start_time = time.time()
        self.verbose = verbose
        self.file_times = []
    
    def update(self, message, percentage):
        """Update progress for a single file."""
        if percentage == 100:
            self.completed_files += 1
            self.file_times.append(time.time())
            
            if self.verbose:
                current_time = time.time()
                elapsed = current_time - self.start_time
                avg_time_per_file = elapsed / self.completed_files if self.completed_files > 0 else 0
                remaining_files = self.total_files - self.completed_files
                eta = avg_time_per_file * remaining_files if remaining_files > 0 else 0
                
                print(f"[{self.completed_files}/{self.total_files}] {message}")
                print(f"  Elapsed: {elapsed:.1f}s | Avg per file: {avg_time_per_file:.1f}s | ETA: {eta:.1f}s")
            else:
                print(f"[{self.completed_files}/{self.total_files}] {message}")
    
    def get_overall_progress(self):
        """Get overall progress percentage."""
        return (self.completed_files / self.total_files) * 100 if self.total_files > 0 else 0

def process_files_parallel(npy_files, output_dir, options, progress_callback=None):
    """Process multiple files in parallel with progress tracking."""
    if options.threads > 1:
        return process_files_parallel_threaded(npy_files, output_dir, options, progress_callback)
    else:
        return process_files_sequential(npy_files, output_dir, options, progress_callback)

def process_files_sequential(npy_files, output_dir, options, progress_callback=None):
    """Process files sequentially."""
    results = []
    progress_tracker = ProgressTracker(len(npy_files), options.progress)
    
    for i, npy_file in enumerate(npy_files):
        if progress_callback:
            overall_progress = (i / len(npy_files)) * 100
            progress_callback(f"Overall: {overall_progress:.1f}% - Processing {os.path.basename(npy_file)}", overall_progress)
        
        try:
            result = extract_data_from_npy(npy_file, output_dir, options, 
                                          lambda msg, pct: progress_tracker.update(msg, pct))
            results.append(result)
        except Exception as e:
            if progress_callback:
                progress_callback(f"Failed to process {npy_file}: {str(e)}", -1)
            results.append({'file': npy_file, 'error': str(e)})
    
    return results

def process_files_parallel_threaded(npy_files, output_dir, options, progress_callback=None):
    """Process files using multiple threads."""
    results = []
    completed_count = 0
    lock = threading.Lock()
    
    def thread_progress_callback(message, percentage):
        """Thread-safe progress callback."""
        nonlocal completed_count
        if percentage == 100:
            with lock:
                completed_count += 1
                if progress_callback:
                    overall_progress = (completed_count / len(npy_files)) * 100
                    progress_callback(f"Overall: {overall_progress:.1f}% - {message}", overall_progress)
        elif progress_callback:
            progress_callback(f"Thread: {message}", -1)
    
    with ThreadPoolExecutor(max_workers=options.threads) as executor:
        # Submit all tasks
        future_to_file = {
            executor.submit(extract_data_from_npy, npy_file, output_dir, options, 
                          thread_progress_callback): npy_file 
            for npy_file in npy_files
        }
        
        # Collect results as they complete
        for future in as_completed(future_to_file):
            npy_file = future_to_file[future]
            try:
                result = future.result()
                results.append(result)
            except Exception as e:
                if progress_callback:
                    progress_callback(f"Thread failed for {npy_file}: {str(e)}", -1)
                results.append({'file': npy_file, 'error': str(e)})
    
    return results

def find_npy_files(input_path):
    """Find all npy files in the given path."""
    npy_files = []
    
    if os.path.isfile(input_path):
        if input_path.endswith('.npy'):
            npy_files.append(input_path)
    elif os.path.isdir(input_path):
        for root, dirs, files in os.walk(input_path):
            for file in files:
                if file.endswith('.npy'):
                    npy_files.append(os.path.join(root, file))
    
    return npy_files

def main():
    """
    Main function with enhanced CLI and progress reporting.
    """
    parser = argparse.ArgumentParser(
        description="Unified Cellpose data extraction script for cellpose2, cellpose3, and cellpose4 formats. "
                   "Extracts data and images from existing npy files with performance optimizations.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Basic usage with automatic version detection
  extract-cellpose-npy.py input.npy output_dir
  
  # Process directory with cellpose3 format
  extract-cellpose-npy.py input_dir output_dir --cellpose-version 3
  
  # Skip ROI extraction and use 4 threads
  extract-cellpose-npy.py input_dir output_dir --skip-roi --threads 4
  
  # Detailed progress reporting
  extract-cellpose-npy.py input_dir output_dir --progress
  
  # Skip specific flow types (memory optimization)
  extract-cellpose-npy.py input_dir output_dir --skip-flows-rgb --skip-flows-dz-dy-dx
  
  # Skip all flows
  extract-cellpose-npy.py input_dir output_dir --skip-flows
  
  # Skip multiple output types
  extract-cellpose-npy.py input_dir output_dir --skip-probabilities --skip-flows-rgb --skip-labels
        """
    )
    
    parser.add_argument("input_files", help="The *.npy file or a directory that contains the *.npy files")
    parser.add_argument("output_dir", help="Directory where the outputs will be stored")
    parser.add_argument("--cellpose-version", choices=["2", "3", "4"],
                       help="Explicitly set cellpose version ('2' for cellpose2, '3' for cellpose3, '4' for cellpose4)")
    parser.add_argument("--skip-roi", help="Skip ROI extraction (performance optimization)", action="store_true")
    parser.add_argument("--skip-probabilities", help="Skip probability extraction", action="store_true")
    parser.add_argument("--skip-flows-rgb", help="Skip RGB flows extraction", action="store_true")
    parser.add_argument("--skip-flows-z", help="Skip Z flows extraction", action="store_true")
    parser.add_argument("--skip-flows-dz-dy-dx", help="Skip dz/dy/dx flows extraction", action="store_true")
    parser.add_argument("--skip-flows", help="Skip all flow extraction (overrides individual flow options)", action="store_true")
    parser.add_argument("--skip-labels", help="Skip label extraction", action="store_true")
    parser.add_argument("--skip-info", help="Skip metadata extraction", action="store_true")
    parser.add_argument("--progress", help="Enable detailed progress reporting", action="store_true")
    parser.add_argument("--threads", type=int, default=1,
                       help="Number of threads for parallel processing (default: 1)")
    
    args = parser.parse_args()
    
    # Validate arguments
    if not os.path.exists(args.input_files):
        print(f"Error: Input path '{args.input_files}' does not exist")
        sys.exit(1)
    
    if args.threads < 1:
        print("Error: Number of threads must be at least 1")
        sys.exit(1)
    
    # Create output directory
    os.makedirs(args.output_dir, exist_ok=True)
    
    # Find all npy files
    npy_files = find_npy_files(args.input_files)
    
    if not npy_files:
        print(f"No .npy files found in '{args.input_files}'")
        sys.exit(1)
    
    print(f"Detected {len(npy_files)} *.npy files:")
    for f in npy_files:
        print(f" - {f}")
    
    print(f"\nProcessing options:")
    print(f"  Cellpose version: {args.cellpose_version or 'auto-detect'}")
    print(f"  Skip ROI: {args.skip_roi}")
    print(f"  Skip probabilities: {args.skip_probabilities}")
    print(f"  Skip flows: {args.skip_flows}")
    print(f"    - Skip RGB flows: {args.skip_flows_rgb}")
    print(f"    - Skip Z flows: {args.skip_flows_z}")
    print(f"    - Skip dz/dy/dx flows: {args.skip_flows_dz_dy_dx}")
    print(f"  Skip labels: {args.skip_labels}")
    print(f"  Skip info: {args.skip_info}")
    print(f"  Threads: {args.threads}")
    print(f"  Progress: {args.progress}")
    
    # Set up progress callback
    progress_callback = progress_callback_detailed if args.progress else progress_callback_simple
    
    # Process files
    start_time = time.time()
    print(f"\nStarting processing...")
    
    try:
        results = process_files_parallel(npy_files, args.output_dir, args, progress_callback)
        
        # Print summary
        total_time = time.time() - start_time
        successful_files = len([r for r in results if 'error' not in r])
        failed_files = len([r for r in results if 'error' in r])
        
        print(f"\nProcessing completed!")
        print(f"  Total files: {len(npy_files)}")
        print(f"  Successful: {successful_files}")
        print(f"  Failed: {failed_files}")
        print(f"  Total time: {total_time:.2f}s")
        print(f"  Average per file: {total_time/len(npy_files):.2f}s")
        
        if failed_files > 0:
            print(f"\nFailed files:")
            for result in results:
                if 'error' in result:
                    print(f"  - {result['file']}: {result['error']}")
        
    except KeyboardInterrupt:
        print("\nProcessing interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\nError during processing: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    # Import threading only when needed for parallel processing
    import threading
    main()