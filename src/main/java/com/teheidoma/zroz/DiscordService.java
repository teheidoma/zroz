package com.teheidoma.zroz;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiscordService implements ApplicationListener<ContextRefreshedEvent> {
    @Value("${discord.token}")
    private String discordToken;
    @Value("${storage.dir:~/zroz}")
    private String path;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;


    public void init() {
        DiscordClient.create(discordToken)
                .withGateway(discord -> discord.on(MessageCreateEvent.class)
                        .filterWhen(message -> message.getMessage().getChannel().map(MessageChannel::getId).map(id -> id.asString().equals("1029727703248678983")))
                        .filter(message -> !message.getMessage().getEmbeds().isEmpty())
                        .flatMap(this::convertAndSend))
                .block();
    }

    private Mono<Void> convertAndSend(MessageCreateEvent event) {
        return convert(event.getMessage().getEmbeds().get(0).getUrl().orElseThrow())
                .flatMap(output -> event.getMessage().getChannel().flatMap(channel -> channel.createMessage(output)))
                .then();
    }

    private Mono<String> convert(String url) {
        String id = UUID.randomUUID().toString();
        String mp4FilePath = path + File.separator + id + "_video.mp4";
        String palletPath = path + File.separator + id + "_pallet.png";
        String gifPath = path + File.separator + id + ".gif";
        return Mono.create(sink -> threadPoolTaskExecutor.execute(() -> {
            try {
                FFmpegExecutor executor = new FFmpegExecutor();
                log.info("drawing text on {}", id);
                executor.createJob(new FFmpegBuilder()
                                .setInput(url)
                                .setVideoFilter("drawtext=fontfile=\"" + path + "/impact.ttf\"" + ":text='Зроз':fontcolor=white:fontsize=24:box=1:boxcolor=black@0.5:boxborderw=5:x=(w-text_w)/2:y=h-th-10")
                                .overrideOutputFiles(true)
                                .addOutput(mp4FilePath)
                                .done())
                        .run();

                executor.createJob(new FFmpegBuilder()
                                .setInput(mp4FilePath)
                                .setVideoFilter("scale=300:-1:flags=lanczos,palettegen=stats_mode=diff")
                                .overrideOutputFiles(true)
                                .addOutput(palletPath)
                                .done())
                        .run();

                executor.createJob(new FFmpegBuilder()
                                .addInput(mp4FilePath)
                                .addInput(palletPath)
                                .setComplexFilter("scale=300:-1:flags=lanczos [x]; [x][1:v] paletteuse")
                                .addOutput(gifPath)
                                .done())
                        .run();

                sink.success(id + ".gif");
            } catch (IOException e) {
                sink.error(e);
            } finally {
                new File(mp4FilePath).delete();
                new File(palletPath).delete();
            }
        }));
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        init();
    }
}
