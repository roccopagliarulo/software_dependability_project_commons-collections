/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.collections4.demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class DemoApp {

    private static final CircularFifoQueue<String> queue = new CircularFifoQueue<>(5);

    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new IndexHandler());
        server.createContext("/api/queue/offer", new OfferHandler());
        server.createContext("/api/queue/poll", new PollHandler());
        server.createContext("/api/queue/status", new StatusHandler());

        server.setExecutor(null);
        System.out.println("Web App avviata con successo su http://localhost:" + port);
        server.start();
    }

    // Serve la pagina HTML con interfaccia grafica moderna
    static class IndexHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = "<!DOCTYPE html>" +
                    "<html lang='it'>" +
                    "<head>" +
                    "<meta charset='UTF-8'>" +
                    "<title>CircularFifoQueue Demo</title>" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; background: #f4f6f9; margin: 40px; }" +
                    ".card { background: white; padding: 25px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); max-width: 600px; margin: auto; }" +
                    "h2 { color: #333; }" +
                    "input[type=text] { width: 70%; padding: 10px; margin-right: 5px; border: 1px solid #ccc; border-radius: 4px; }" +
                    "button { padding: 10px 15px; border: none; border-radius: 4px; cursor: pointer; font-weight: bold; }" +
                    ".btn-add { background: #28a745; color: white; }" +
                    ".btn-poll { background: #dc3545; color: white; margin-top: 10px; }" +
                    ".status-box { background: #e9ecef; padding: 15px; border-radius: 4px; margin-top: 20px; font-family: monospace; }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<div class='card'>" +
                    "<h2>Demo: CircularFifoQueue (Capacità = 5)</h2>" +
                    "<div>" +
                    "<input type='text' id='elemInput' placeholder='Inserisci elemento...'>" +
                    "<button class='btn-add' onclick='addElement()'>Aggiungi (Offer)</button>" +
                    "</div>" +
                    "<div>" +
                    "<button class='btn-poll' onclick='pollElement()'>Estrai di testa (Poll)</button>" +
                    "</div>" +
                    "<div class='status-box' id='statusBox'>Caricamento stato...</div>" +
                    "</div>" +
                    "<script>" +
                    "async function updateStatus() {" +
                    "  const res = await fetch('/api/queue/status');" +
                    "  const data = await res.json();" +
                    "  document.getElementById('statusBox').innerHTML = " +
                    "    '<b>Dimensione:</b> ' + data.size + '/' + data.maxSize + '<br>' +" +
                    "    '<b>Piena:</b> ' + (data.isFull ? '<span style=\"color:red;\">SI</span>' : 'NO') + '<br>' +" +
                    "    '<b>Elementi:</b> ' + JSON.stringify(data.elements);" +
                    "}" +
                    "async function addElement() {" +
                    "  const input = document.getElementById('elemInput');" +
                    "  if(!input.value) return;" +
                    "  await fetch('/api/queue/offer?element=' + encodeURIComponent(input.value), { method: 'POST' });" +
                    "  input.value = '';" +
                    "  updateStatus();" +
                    "}" +
                    "async function pollElement() {" +
                    "  const res = await fetch('/api/queue/poll', { method: 'POST' });" +
                    "  const data = await res.json();" +
                    "  if(data.extracted) alert('Estratto: ' + data.extracted);" +
                    "  else alert('La coda è vuota!');" +
                    "  updateStatus();" +
                    "}" +
                    "updateStatus();" +
                    "</script>" +
                    "</body>" +
                    "</html>";

            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class OfferHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.startsWith("element=")) {
                    String value = query.substring("element=".length());
                    queue.offer(value);
                }
            }
            sendJsonResponse(exchange, "{\"status\":\"ok\"}");
        }
    }

    static class PollHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String extracted = queue.poll();
            String json = (extracted == null) ? "{\"extracted\":null}" : "{\"extracted\":\"" + extracted + "\"}";
            sendJsonResponse(exchange, json);
        }
    }

    static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder elementsJson = new StringBuilder("[");
            int i = 0;
            for (String elem : queue) {
                if (i > 0) elementsJson.append(",");
                elementsJson.append("\"").append(elem).append("\"");
                i++;
            }
            elementsJson.append("]");

            String json = "{" +
                    "\"size\":" + queue.size() + "," +
                    "\"maxSize\":" + queue.maxSize() + "," +
                    "\"isFull\":" + queue.isAtFullCapacity() + "," +
                    "\"elements\":" + elementsJson +
                    "}";
            sendJsonResponse(exchange, json);
        }
    }

    private static void sendJsonResponse(HttpExchange exchange, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}