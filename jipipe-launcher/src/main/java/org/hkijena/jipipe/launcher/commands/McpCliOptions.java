package org.hkijena.jipipe.launcher.commands;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsed {@code --mcp} command line options.
 * <p>
 * Syntax: {@code --mcp [port]} and {@code --mcp-permissions allow=<ids> deny=<ids>}.
 * Repeated flags accumulate; unknown permission keys are ignored. Port is optional: without
 * it, the port is resolved from {@code MCPApplicationSettings} at startup.
 * <p>
 * {@link #consumedCount(List, int)} is the single source of truth for which arguments belong
 * to an MCP flag: the parser and {@link GuiCommand} argument filtering both use it, so MCP
 * arguments never leak into the remaining (project path) arguments.
 */
public record McpCliOptions(boolean requested, Integer port, List<String> allowIds, List<String> denyIds) {

    /**
     * Parses the MCP options from the full argument list.
     */
    public static McpCliOptions parse(List<String> args) {
        boolean requested = false;
        Integer port = null;
        List<String> allowIds = new ArrayList<>();
        List<String> denyIds = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            int consumed = consumedCount(args, i);
            if (consumed <= 0) {
                continue;
            }
            String arg = args.get(i);
            if ("--mcp".equals(arg)) {
                requested = true;
                if (consumed > 1) {
                    port = Integer.parseInt(args.get(i + 1));
                }
            } else if ("--mcp-permissions".equals(arg)) {
                for (int j = 1; j < consumed; j++) {
                    String spec = args.get(i + j);
                    int separator = spec.indexOf('=');
                    String key = spec.substring(0, separator);
                    List<String> ids = parseIds(spec.substring(separator + 1));
                    switch (key) {
                        case "allow" -> allowIds.addAll(ids);
                        case "deny" -> denyIds.addAll(ids);
                        // Unknown keys are ignored.
                    }
                }
            }
            i += consumed - 1;
        }
        return new McpCliOptions(requested, port, List.copyOf(allowIds), List.copyOf(denyIds));
    }

    /**
     * Number of arguments starting at index i that belong to an MCP flag (0 = none).
     * {@code --mcp} consumes a following positive integer port; {@code --mcp-permissions}
     * consumes following {@code key=value} specs until the next flag or non-spec argument.
     */
    public static int consumedCount(List<String> args, int i) {
        String arg = args.get(i);
        if ("--mcp".equals(arg)) {
            if (i + 1 < args.size()) {
                try {
                    if (Integer.parseInt(args.get(i + 1)) > 0) {
                        return 2;
                    }
                } catch (NumberFormatException e) {
                    // The next argument is not a port and stays with its owner.
                }
            }
            return 1;
        }
        if ("--mcp-permissions".equals(arg)) {
            int count = 1;
            while (i + count < args.size()) {
                String spec = args.get(i + count);
                int separator = spec.indexOf('=');
                if (separator <= 0 || spec.startsWith("-")) {
                    break;
                }
                count++;
            }
            return count;
        }
        return 0;
    }

    private static List<String> parseIds(String value) {
        List<String> ids = new ArrayList<>();
        for (String id : value.split(",")) {
            String trimmed = id.trim();
            if (!trimmed.isEmpty()) {
                ids.add(trimmed);
            }
        }
        return ids;
    }
}
